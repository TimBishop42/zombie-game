# Project Plan — Walking Dead Grid Simulation

Companion to `CLAUDE.md`, which holds the constraints and decision log this
plan is built from. This document is the concrete shape: stack, architecture,
model, API/CLI surface, testing strategy, and the implementation sequence.

## 1. Tech stack

| Concern | Choice |
|---|---|
| Language | Java 26 (`--enable-preview` for Structured Concurrency) |
| Framework | Spring Boot 4.1.1 (Spring Framework 7, Jakarta EE 11) |
| Build | Gradle, Kotlin DSL |
| Web | Spring Web (REST) |
| Persistence | Spring Data JPA + Hibernate, SQLite (`org.xerial:sqlite-jdbc`, `hibernate-community-dialects`) |
| Test | JUnit 5, AssertJ |
| Performance | JMH (`me.champeau.jmh` Gradle plugin) |
| Lint | Checkstyle (Gradle built-in plugin, Google Java style ruleset) |
| Task runner | Just (`justfile`) |

Single-module Gradle project. A multi-module split isn't earned at this
scope — package boundaries (below) give the same separation without the
build-complexity overhead.

**Developer workflow**: a `justfile` at repo root wraps the Gradle tasks so
day-to-day commands stay short and memorable, with Gradle remaining the
actual source of truth (no logic lives in the justfile itself):

```
just build              → ./gradlew build
just test               → ./gradlew test
just lint               → ./gradlew checkstyleMain checkstyleTest
just performance-test    → ./gradlew jmh (added in step 12, once JMH is wired in)
just run                 → ./gradlew bootRun
just cli <args>          → ./gradlew bootRun --args='--spring.profiles.active=cli <args>'
just clean               → ./gradlew clean
just verify              → just lint && just test && just build
```

(`./gradlew build` already runs `check` — test and Checkstyle — internally,
so `just verify` has some harmless overlap with `just build` alone; it's kept
as an explicit alias because the per-step workflow below calls it out by
name.)

## 2. Module/package structure

```
com.zombiegame
├── engine/            pure domain + simulation logic — zero Spring dependency
│   ├── actor/          Actor (sealed abstract), Scientist, PoliceOfficer, Zombie
│   ├── grid/           Coordinate (record), Grid, Cell, SpatialIndex (bucket-hash)
│   ├── resource/       ResourceNode
│   ├── behavior/       MovementStrategy + default heuristic implementation
│   ├── interaction/     InteractionPolicy, IndependentTeamsPolicy, InteractionResolver,
│   │                    InteractionOutcome (sealed: Infected, Eliminated, NoEffect, ResourceDropped)
│   ├── turn/           TurnEngine, TurnPhase, TurnResult
│   ├── game/           Game, GameConfig, GameStatus, GameResult
│   └── random/         SeededRandom (wraps java.util.random.RandomGenerator)
├── persistence/
│   ├── entity/         JPA entities (distinct from engine domain types)
│   ├── repository/     Spring Data repositories
│   └── mapper/         domain <-> entity mapping
├── api/
│   ├── GameController
│   └── dto/            request/response DTOs (never expose engine or entity types directly)
├── cli/
│   └── GameCliRunner    CommandLineRunner, active under a "cli" Spring profile
└── config/              Spring configuration (JPA, async/concurrency executor)
```

This is the target shape, populated incrementally — sub-packages are created
in the step that first adds a class into them (starting with `engine` in step
2), not pre-created as empty scaffolding in step 1, per the "no bloat"
principle in `CLAUDE.md`.

`engine` depends on nothing else in the project. `persistence`, `api`, and
`cli` depend on `engine`, never the reverse. This is what makes the domain
logic unit-testable with no Spring context, and keeps storage/transport
concerns from leaking into game rules.

## 3. Domain model

- **`Actor`** — sealed abstract class, `permits Scientist, PoliceOfficer,
  Zombie`. Holds id, `Coordinate` position, alive/state, and (for humans) a
  carried `ResourceNode` reference. `Zombie` additionally holds chase state:
  a nullable locked target actor id and remaining chase distance (see
  `MovementStrategy` below) — state specific to that subtype, not shared.
- **`Coordinate(int x, int y)`** — record, used directly as a map key.
- **`Grid`** — sparse `Map<Coordinate, Cell>`; a `Cell` may hold a
  `ResourceNode`. Chosen over a dense 2D array so memory scales with occupied
  cells, not N².
- **`SpatialIndex`** — bucket-hash (`Map<BucketCoordinate, List<Actor>>`) over
  live actor positions, rebuilt each turn, used for radius queries (humans
  sensing zombies within `GameConfig.humanSensingRadius`; zombies detecting
  humans within `GameConfig.zombieLockOnRadius`). API/state responses only
  ever enumerate occupied entities (actors, resource nodes) plus `gridSize` —
  never a serialized grid array — so the sparse representation carries
  straight through to the wire format; a client draws the empty grid itself
  from `gridSize` and overlays entities at their given coordinates.
- **`MovementStrategy`** — one implementation per actor "intent":
  - **Scientist / PoliceOfficer**: if carrying a resource, head to home edge;
    else head to the nearest sensed resource node; if a zombie is sensed
    within `humanSensingRadius`, that takes priority; otherwise a random step.
  - **Zombie — chase mechanic**: if currently locked onto a target (target
    alive and remaining chase distance > 0), move toward that target's
    current position regardless of distance, decrementing remaining chase
    distance by cells moved; the lock releases on catching the target, on
    exhausting the chase distance, or if the target dies first. If unlocked,
    scan `zombieLockOnRadius` for humans and lock onto the nearest one
    (deterministic tie-break by actor id), setting remaining chase distance to
    `zombieChaseDistance`; otherwise a random step. Multiple zombies may lock
    onto the same human — no exclusivity.

  This is the "simple behavior pattern" for v1 and is the deliberate
  extension point for smarter behavior later.
- **`InteractionPolicy`** — strategy selected in `GameConfig`
  (`IndependentTeamsPolicy` for v1); governs whether Police and Scientists can
  affect each other at all.
- **`InteractionResolver`** — sealed-type pattern-matching `switch` resolving
  a same-cell encounter into an `InteractionOutcome`. Zombie-vs-Scientist →
  `Infected`; Zombie-vs-PoliceOfficer → seeded-random roll against
  `GameConfig.policeCombatWinChance`, resolving to `Eliminated` (zombie removed)
  or `Infected` (officer turns). An `Infected` outcome carrying a resource also
  emits `ResourceDropped` at that cell.
- **`TurnEngine`** — runs one turn as three sequential sub-phases (Scientists,
  Police, Zombies). Each sub-phase splits into a parallel decision step (every
  actor in that phase computes its intended move against an immutable
  snapshot, via Structured Concurrency) and a sequential, fixed-order
  (actor-id-sorted) apply step that mutates state and resolves interactions.
  This is what keeps parallel evaluation reproducible under a fixed seed.
- **`Game`** — aggregate root: `GameConfig`, `SeededRandom`, current grid/actor
  state, `GameStatus`, running score per human team, and the accumulated
  `TurnResult` history (for replay).
- **`GameStatus`** — `IN_PROGRESS`, `SCIENTISTS_WON`, `POLICE_WON`, `TIE`
  (all resources collected, both teams alive, equal score — flagging this as
  an added edge case not explicit in the original rules).

## 4. Configuration

`GameConfig` is the single place all tunable parameters live, all seed-driven
where randomness is involved:

| Field | Default |
|---|---|
| `gridSize` | 20 |
| `resourceNodeCount` | 15 |
| `scientistCount` / `policeCount` | 5 / 5 |
| `initialZombieCount` | 3 |
| `movementRange` (per actor type) | 1 |
| `humanSensingRadius` | 3 |
| `zombieLockOnRadius` | 3 |
| `zombieChaseDistance` | 5 |
| `policeCombatWinChance` | 0.5 |
| `interactionPolicy` | `INDEPENDENT` |
| `seed` | random if omitted, otherwise fixed for reproducible/replayable runs |

## 5. API and CLI surface

Every turn is represented as a **delta**, not a grid snapshot: which actors
moved where, and what interactions/outcomes occurred (infections,
eliminations, pickups, drops, bank events). This is what keeps payloads
proportional to turn-count × active-actor-count rather than to grid size —
a huge grid with modest actor counts costs almost nothing extra.

**REST** (`api` package):
- `POST /games` — create a game from a `GameConfig` (all fields optional,
  defaults above apply); returns initial state.
- `POST /games/{id}/turns` — advance one turn; returns that turn's delta. The
  core primitive — also what the CLI and the batch/stream endpoints below are
  built from, so there is exactly one place turn-stepping logic lives.
- `POST /games/{id}/run` — loop the step primitive to completion in one
  request; returns the full ordered list of turn deltas. The default way to
  get a whole game's worth of replay data — a client (including a future UI)
  can fetch once and animate client-side at its own pace.
- `GET /games/{id}` — current snapshot (scores, status, live actor
  positions).
- `GET /games/{id}/turns` — full persisted turn-delta history for a completed
  game (replay from storage).
- `GET /games/{id}/stream` — Server-Sent Events (`text/event-stream`) via
  Spring MVC's `SseEmitter`, one event per turn delta, pushed as the loop
  produces them. Deliberately **not** WebFlux — `SseEmitter` gives server-push
  streaming from the same servlet-based controller stack without running two
  reactive web frameworks side by side. This is the endpoint that makes very
  large/long-running games observable without blocking on `/run` or holding
  one giant response in memory, and is what a future UI would use to
  watch a live game rather than only replay a finished one.

**CLI** (`cli` package): a `CommandLineRunner` active under `--spring.profiles.active=cli`,
taking the same config as flags/args, running a game to completion, and
logging each turn's summary plus the final result to console. Shares
`GameConfig`/`engine` with the REST path — no duplicated game logic.

## 6. Persistence

- JPA entities live only in `persistence.entity`, mapped from/to `engine`
  domain types via `persistence.mapper` — the engine never sees an entity or
  a JPA annotation.
- SQLite via `org.xerial:sqlite-jdbc` + `hibernate-community-dialects`
  `SQLiteDialect`, wired through standard Spring `application.yml` datasource
  properties — moving to Postgres later is a dependency/config change only.
- Persisted per game: config + seed, status/result, and the turn history
  needed for replay.

## 7. Testing strategy

- `engine`: plain JUnit 5 + AssertJ, no Spring context. Covers movement
  decisions, interaction resolution (all branches, including the seeded
  combat roll), turn sequencing, spatial index correctness, and win/tie
  conditions.
- **Determinism test**: same seed run twice end-to-end produces an identical
  turn history — this is the test that actually validates the seeding design
  works, not just that it compiles.
- `persistence`: `@DataJpaTest` against SQLite, repository round-trip checks.
- `api`: `@SpringBootTest` + `MockMvc`/`WebTestClient`, thin — wiring only,
  not re-testing engine rules already covered above.
- `cli`: one smoke test running a small game end-to-end via the runner.

**Performance suite** (separate `src/jmh/java` source set, via the
`me.champeau.jmh` Gradle plugin — kept out of `main`/`test` so benchmarks
never ship in the app jar or slow down the regular test run):
- Hand-rolled timing (`System.nanoTime()` around a loop) is deliberately not
  used — JIT warmup and GC noise make it unreliable and non-comparable
  run-to-run. JMH exists specifically to control for that.
- Benchmarks: full game completion time (`@BenchmarkMode(Mode.AverageTime)`)
  across a few representative `GameConfig` presets (small / medium /
  large-scale grid+actor counts), the last of which is what actually
  exercises the "build for scale" data structures and the parallel decision
  phase.
- Memory: JMH's `-prof gc` profiler on the same benchmarks — allocation rate
  and GC pause count/time as the memory-footprint signal.
- Results written as JSON (`./gradlew jmh`, results under
  `build/reports/jmh/`).
- **Regression automation**: a small `BenchmarkComparator` utility diffs the
  latest `results.json` against a checked-in `perf/baseline.json`, prints
  per-benchmark percentage deltas, and exits non-zero if any benchmark
  regresses past a configurable threshold. Workflow: run `./gradlew jmh`
  before and after a targeted optimization, compare, and only update the
  committed baseline once a change is confirmed to actually help — this is
  what makes the suite something you *use* to drive optimization work, not
  just a one-off measurement.

## 8. Implementation sequence

Ordered for a single day; each step should be independently testable before
moving on.

1. Gradle scaffold — Spring Boot app skeleton (`com.zombiegame` root package
   only; sub-packages arrive with their first class in later steps),
   preview-features flag, dependencies, Checkstyle wiring, and the `justfile`.
2. Core types: `Coordinate`, `Grid`, `Actor` hierarchy, `ResourceNode`,
   `GameConfig`, `SeededRandom`.
3. Initial placement: edge spawning for Scientists/Police, random resource
   placement — all seed-driven.
4. `SpatialIndex` + `MovementStrategy` default heuristic.
5. `InteractionResolver` + `IndependentTeamsPolicy` + resource
   pickup/drop/bank logic.
6. `TurnEngine` (sequential phases; parallel decide / sequential apply) +
   `GameStatus` evaluation.
7. Determinism test suite — confirms steps 2–6 are actually reproducible.
8. Persistence layer (entities, repositories, SQLite config, mapper).
9. REST API layer: create/step/run/snapshot/history endpoints first, then the
   `SseEmitter` stream endpoint.
10. CLI runner.
11. End-to-end pass: run a full game via CLI, via REST `/run`, and via the SSE
    stream, sanity-check all three agree.
12. JMH performance suite: `me.champeau.jmh` plugin wiring, the
    `performance-test` justfile recipe, benchmark presets, GC profiler,
    baseline JSON, and the `BenchmarkComparator` regression check.

React UI itself is out of scope for this pass (stretch goal only) — but
unlike the UI, the streaming endpoint is being built now precisely so the
backend doesn't need revisiting when that UI work starts.

### Status log

Each step is one commit, per the per-step development workflow in
`CLAUDE.md`. Updated after each step is committed.

| Step | Description | Status | Commit |
|---|---|---|---|
| 1 | Gradle scaffold, Checkstyle, justfile | Not started | |
| 2 | Core types | Not started | |
| 3 | Initial placement | Not started | |
| 4 | SpatialIndex + MovementStrategy | Not started | |
| 5 | InteractionResolver + IndependentTeamsPolicy | Not started | |
| 6 | TurnEngine + GameStatus | Not started | |
| 7 | Determinism test suite | Not started | |
| 8 | Persistence layer | Not started | |
| 9 | REST API layer | Not started | |
| 10 | CLI runner | Not started | |
| 11 | End-to-end pass | Not started | |
| 12 | JMH performance suite | Not started | |

## 9. Open assumption to confirm

`TIE` as a `GameStatus` wasn't explicit in the original rules (which only
name "most resources" or "sole remaining team" as winning conditions) — added
here to make the state machine total. Flag if you'd rather resolve ties some
other way (e.g. sudden-death extra turns).
