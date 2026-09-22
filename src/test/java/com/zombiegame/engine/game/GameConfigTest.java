package com.zombiegame.engine.game;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zombiegame.engine.interaction.InteractionPolicyType;
import org.junit.jupiter.api.Test;

class GameConfigTest {

    @Test
    void defaultsMatchTheProjectPlan() {
        GameConfig config = GameConfig.defaults(1234L);

        assertThat(config.gridSize()).isEqualTo(20);
        assertThat(config.resourceNodeCount()).isEqualTo(15);
        assertThat(config.scientistCount()).isEqualTo(5);
        assertThat(config.policeCount()).isEqualTo(5);
        assertThat(config.initialZombieCount()).isEqualTo(3);
        assertThat(config.humanMovementRange()).isEqualTo(1);
        assertThat(config.zombieMovementRange()).isEqualTo(1);
        assertThat(config.humanSensingRadius()).isEqualTo(3);
        assertThat(config.zombieLockOnRadius()).isEqualTo(3);
        assertThat(config.zombieChaseDistance()).isEqualTo(5);
        assertThat(config.policeCombatWinChance()).isEqualTo(0.5);
        assertThat(config.interactionPolicy()).isEqualTo(InteractionPolicyType.INDEPENDENT);
        assertThat(config.seed()).isEqualTo(1234L);
    }

    @Test
    void rejectsNonPositiveGridSize() {
        assertThatThrownBy(() -> withGridSize(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeActorCounts() {
        assertThatThrownBy(() -> newConfig(-1, 5, 3)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNonPositiveMovementRanges() {
        assertThatThrownBy(() -> withMovementRanges(0, 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> withMovementRanges(1, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsCombatWinChanceOutsideUnitRange() {
        assertThatThrownBy(() -> withCombatWinChance(1.5))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNaNCombatWinChance() {
        assertThatThrownBy(() -> withCombatWinChance(Double.NaN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullInteractionPolicy() {
        assertThatThrownBy(
                        () -> new GameConfig(20, 15, 5, 5, 3, 1, 1, 3, 3, 5, 0.5, null, 1L))
                .isInstanceOf(NullPointerException.class);
    }

    private static GameConfig withGridSize(int gridSize) {
        return new GameConfig(gridSize, 15, 5, 5, 3, 1, 1, 3, 3, 5, 0.5,
                InteractionPolicyType.INDEPENDENT, 1L);
    }

    private static GameConfig withMovementRanges(int humanMovementRange, int zombieMovementRange) {
        return new GameConfig(20, 15, 5, 5, 3, humanMovementRange, zombieMovementRange, 3, 3, 5,
                0.5, InteractionPolicyType.INDEPENDENT, 1L);
    }

    private static GameConfig withCombatWinChance(double policeCombatWinChance) {
        return new GameConfig(20, 15, 5, 5, 3, 1, 1, 3, 3, 5, policeCombatWinChance,
                InteractionPolicyType.INDEPENDENT, 1L);
    }

    private static GameConfig newConfig(int scientistCount, int policeCount, int zombieCount) {
        return new GameConfig(20, 15, scientistCount, policeCount, zombieCount, 1, 1, 3, 3, 5, 0.5,
                InteractionPolicyType.INDEPENDENT, 1L);
    }
}
