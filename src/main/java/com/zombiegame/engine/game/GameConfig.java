package com.zombiegame.engine.game;

import com.zombiegame.engine.interaction.InteractionPolicyType;
import java.util.Objects;

public record GameConfig(
        int gridSize,
        int resourceNodeCount,
        int scientistCount,
        int policeCount,
        int initialZombieCount,
        int humanMovementRange,
        int zombieMovementRange,
        int humanSensingRadius,
        int zombieLockOnRadius,
        int zombieChaseDistance,
        double policeCombatWinChance,
        InteractionPolicyType interactionPolicy,
        long seed) {

    public GameConfig {
        Objects.requireNonNull(interactionPolicy, "interactionPolicy");
        if (gridSize <= 0) {
            throw new IllegalArgumentException("gridSize must be positive: " + gridSize);
        }
        if (resourceNodeCount < 0) {
            throw new IllegalArgumentException(
                    "resourceNodeCount cannot be negative: " + resourceNodeCount);
        }
        if (scientistCount < 0 || policeCount < 0 || initialZombieCount < 0) {
            throw new IllegalArgumentException("actor counts cannot be negative");
        }
        if (humanMovementRange <= 0 || zombieMovementRange <= 0) {
            throw new IllegalArgumentException("movement ranges must be positive");
        }
        if (humanSensingRadius < 0 || zombieLockOnRadius < 0 || zombieChaseDistance < 0) {
            throw new IllegalArgumentException("radii/distances cannot be negative");
        }
        if (!(policeCombatWinChance >= 0.0 && policeCombatWinChance <= 1.0)) {
            throw new IllegalArgumentException(
                    "policeCombatWinChance must be between 0 and 1: " + policeCombatWinChance);
        }
    }

    public static GameConfig defaults(long seed) {
        int gridSize = 20;
        int resourceNodeCount = 15;
        int scientistCount = 5;
        int policeCount = 5;
        int initialZombieCount = 3;
        int humanMovementRange = 1;
        int zombieMovementRange = 1;
        int humanSensingRadius = 3;
        int zombieLockOnRadius = 3;
        int zombieChaseDistance = 5;
        double policeCombatWinChance = 0.5;
        InteractionPolicyType interactionPolicy = InteractionPolicyType.INDEPENDENT;

        return new GameConfig(
                gridSize,
                resourceNodeCount,
                scientistCount,
                policeCount,
                initialZombieCount,
                humanMovementRange,
                zombieMovementRange,
                humanSensingRadius,
                zombieLockOnRadius,
                zombieChaseDistance,
                policeCombatWinChance,
                interactionPolicy,
                seed);
    }
}
