package com.zombiegame.engine.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zombiegame.engine.grid.Coordinate;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class ZombieTest {

    @Test
    void startsWithNoLockedTarget() {
        Zombie zombie = new Zombie(1, new Coordinate(10, 10));

        assertThat(zombie.hasLockedTarget()).isFalse();
        assertThat(zombie.lockedTargetId()).isEmpty();
        assertThat(zombie.remainingChaseDistance()).isZero();
    }

    @Test
    void locksOntoTarget() {
        Zombie zombie = new Zombie(1, new Coordinate(10, 10));

        zombie.lockOnto(42, 5);

        assertThat(zombie.hasLockedTarget()).isTrue();
        assertThat(zombie.lockedTargetId()).isEqualTo(OptionalInt.of(42));
        assertThat(zombie.remainingChaseDistance()).isEqualTo(5);
    }

    @Test
    void rejectsLockingOnWithNegativeChaseDistance() {
        Zombie zombie = new Zombie(1, new Coordinate(10, 10));

        assertThatThrownBy(() -> zombie.lockOnto(42, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLockingOnWhileAlreadyLocked() {
        Zombie zombie = new Zombie(1, new Coordinate(10, 10));
        zombie.lockOnto(42, 5);

        assertThatThrownBy(() -> zombie.lockOnto(99, 3))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void chaseDistanceNeverGoesNegative() {
        Zombie zombie = new Zombie(1, new Coordinate(10, 10));
        zombie.lockOnto(42, 2);

        zombie.reduceChaseDistance(5);

        assertThat(zombie.remainingChaseDistance()).isZero();
    }

    @Test
    void rejectsReducingChaseDistanceByANegativeAmount() {
        Zombie zombie = new Zombie(1, new Coordinate(10, 10));
        zombie.lockOnto(42, 5);

        assertThatThrownBy(() -> zombie.reduceChaseDistance(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void releasingClearsTheLock() {
        Zombie zombie = new Zombie(1, new Coordinate(10, 10));
        zombie.lockOnto(42, 5);

        zombie.releaseLock();

        assertThat(zombie.hasLockedTarget()).isFalse();
        assertThat(zombie.lockedTargetId()).isEmpty();
        assertThat(zombie.remainingChaseDistance()).isZero();
    }
}
