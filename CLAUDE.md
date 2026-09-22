# CLAUDE.md

Guidance for working in this repository. This is a take-home exercise being used
as a deliberate refresher on modern Java and Spring Boot after ~4 years away from
the ecosystem (last touched Java 11/13, Spring Framework 5.x, Spring Boot 2.x).

## Purpose and priorities

1. **Learning is the primary objective.** Prefer idiomatic modern Java and Spring
   Boot patterns over familiar older ones, where they genuinely fit the problem.
   Don't force a new feature in where it doesn't belong.
2. **Clean and simple over clever.** Code must be easily readable and parsable by
   a human reviewer, not optimized for an agent. Follow current industry-standard
   Java project structure and conventions throughout.
3. **No bloat.** No speculative abstractions, no framework ceremony for its own
   sake, no defensive handling of scenarios that cannot occur, no building ahead
   of stated requirements (stretch goals are noted but not implemented early).

## Process

Work proceeds in explicit stages, in order:

1. `CLAUDE.md` (this file) — constraints and conventions.
2. Project plan — tech stack, architecture, design constraints, testing
   requirements, implementation plan.
3. Implementation.

Do not skip ahead to implementation before the project plan is agreed.

### Per-step development workflow

Each numbered item in the project plan's implementation sequence (section 8)
is exactly one commit, worked through this loop:

1. Implement the step.
2. Run `just verify` (lint + test + build) and, **in parallel**, an
   adversarial review of the step's diff via the `code-review` skill (high
   effort) — both kicked off together, not one after the other.
3. Fix anything either surfaces: failing lint/test/build, or review findings.
4. Hand off for human review. Requested changes are applied (by either party)
   and steps 2–4 repeat until clean.
5. Human commits and pushes.
6. Human gives the go-ahead for the next step; the status log in
   `PROJECT_PLAN.md` section 8 is updated; the loop restarts.

No step starts before the previous one is committed.

## Tech stack (summary — see project plan for detail)

- **Java 26** — latest feature release (not LTS; JDK 27 is imminent). Chosen
  deliberately to explore the newest language features even though a real
  employer's production stack would more likely run an LTS (21/25).
- **Spring Boot 4.1.1** (Spring Framework 7, Jakarta EE 11).
- **Gradle** build.
- Both a **REST API** and a **CLI runner** are first-class deliverables, sharing
  one core simulation engine. A React UI is a stretch goal only — the API should
  be shaped so it's *possible* later (e.g. exposing turn history for replay), but
  no frontend work happens now.

## Code style

- Favor records, sealed interfaces/classes, and pattern matching (`switch` on
  sealed types, record patterns) for the domain model where they clarify intent.
- No Lombok — modern records/accessors remove most of the need for it, and using
  it would work against the "explore current Java" goal.
- Comments only where the *why* isn't obvious from the code (a non-obvious
  constraint, a subtle invariant). Never comment what the code already says.
- No javadoc-style boilerplate on self-explanatory methods.
- Package by feature/domain, not by technical layer-only buckets.

## Testing

- JUnit 5 + AssertJ as the baseline.
- Domain/simulation logic (movement, interactions, turn resolution, win
  conditions) is deterministic and must be unit-testable in isolation from
  Spring — no `@SpringBootTest` needed for pure game logic.
- Thin integration tests around the REST layer and CLI entry point to confirm
  wiring, not to re-test domain rules already covered elsewhere.

## Technical architecture decisions

- **Interaction dispatch:** actor-vs-actor encounters are resolved via sealed
  types (`Actor permits Human, Zombie`; `Human permits Scientist,
  PoliceOfficer`) and pattern-matching `switch`, not a classic Visitor. This
  gives compiler-enforced exhaustiveness when a new actor type is added, with
  far less boilerplate than accept/visit methods. The hierarchy is two levels
  deep rather than a flat three-way `permits` — `Human` exists to hold the
  carried-resource state/behavior shared by `Scientist` and `PoliceOfficer`
  once, instead of duplicating it.
- **Team relationship is swappable:** Police-vs-Scientist behavior is chosen via
  an `InteractionPolicy` strategy (starting with an independent-teams policy),
  so it can later be swapped for an adversarial policy without touching engine
  code.
- **Persistence:** Spring Data JPA + Hibernate, SQLite now
  (`org.xerial:sqlite-jdbc` + `hibernate-community-dialects`), Postgres later —
  a dialect/config swap only. Domain model stays free of JPA annotations;
  persistence has its own entity/mapper layer (ports-and-adapters), so the game
  engine has zero dependency on storage technology. Persisted data: game
  config + seed + result, and full turn-by-turn event history (to support
  replay).
- **Determinism:** one seeded `RandomGenerator` (`java.util.random`, JEP 356)
  per game, threaded through every source of randomness — no ad hoc `new
  Random()` calls. Actor processing order within a phase is always an explicit
  stable order (e.g. sorted by actor ID), never hash-based iteration order.
- **Grid structure:** sparse `Map<Coordinate, Cell>` (`Coordinate` a record
  used as the hash key), not a dense 2D array — a dense array's memory cost
  scales with N² regardless of occupancy, which breaks down at the large grid
  sizes we're designing for.
- **Spatial queries:** a spatial bucket-hash (`Map<BucketCoordinate,
  List<Actor>>`) over actor positions supports fast radius queries with cheap
  per-turn rebuilds — simpler and cheaper to maintain each turn than a
  quadtree, given actors move every turn.
- **Parallelism vs. determinism:** each phase splits into a **parallel,
  read-only decision step** (every actor decides its move from an immutable
  snapshot of current state — this is what gets parallelized) followed by a
  **sequential, fixed-order apply step** (conflict resolution and state
  mutation), so parallel execution never changes the outcome.
- **Concurrency mechanism:** JDK 26's preview Structured Concurrency API is
  used for the parallel decision step, deliberately chosen over stable
  parallel streams to explore the newest Java concurrency model. This requires
  `--enable-preview` in the Gradle build (and matching IDE/run config), and the
  API may still change before finalization in a future JDK — acceptable
  tradeoff given the learning-focused goal of this project.

## Domain decision log

Captured here as they're made, so design intent isn't lost. See the project
plan for how these translate into the actual model.

- **Actors:** three groups — Scientists, Police, Zombies — on an N×N grid with a
  configurable number of randomly placed resource nodes.
- **Starting positions:** Scientists spawn on the left edge, Police on the right
  edge, each spaced randomly along the vertical axis.
- **Bases:** each human team's base is its home edge/column — returning to any
  cell on that edge banks a carried resource.
- **Carrying:** one resource at a time; an actor must return to base to bank it
  before picking up another.
- **Movement:** configurable range per actor type (e.g. zombies may move at a
  different rate than humans).
- **Turn structure:** sequential sub-phases per turn — Scientists move and are
  evaluated, then Police move and are evaluated, then Zombies move and are
  evaluated.
- **Zombie contact:** outcome depends on the human's type.
  - **Scientist:** instant infection — becomes a zombie.
  - **Police officer:** fights back — a configurable `x%` chance of eliminating
    the zombie, otherwise the officer is infected and becomes a zombie.
- **Infection:** a human that is infected (rather than eliminating the zombie)
  becomes a new zombie, growing the zombie population over the game. If the
  infected actor was carrying a resource, that resource is dropped at the cell
  where the infection occurred, and can be picked up again by another actor.
- **Police vs. Scientists:** fully independent teams — no direct interaction;
  they compete only indirectly for the shared, finite resource pool.
- **End conditions:** the game ends when all resources have been collected, or
  when one human team (Police or Scientists) is fully eliminated.
- **Win condition:** if one human team is fully eliminated, the other team wins
  outright, regardless of resources already banked. Otherwise, when resources
  run out with both teams still alive, the team with more resources banked
  wins.
