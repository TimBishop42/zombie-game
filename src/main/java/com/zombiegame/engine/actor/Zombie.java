package com.zombiegame.engine.actor;

import com.zombiegame.engine.grid.Coordinate;
import java.util.OptionalInt;

public final class Zombie extends Actor {

    private Integer lockedTargetId;
    private int remainingChaseDistance;

    public Zombie(int id, Coordinate position) {
        super(id, position);
    }

    public boolean hasLockedTarget() {
        return lockedTargetId != null;
    }

    public OptionalInt lockedTargetId() {
        return lockedTargetId == null ? OptionalInt.empty() : OptionalInt.of(lockedTargetId);
    }

    public int remainingChaseDistance() {
        return remainingChaseDistance;
    }

    public void lockOnto(int targetId, int chaseDistance) {
        if (hasLockedTarget()) {
            throw new IllegalStateException("Already locked onto target: " + lockedTargetId);
        }
        if (chaseDistance < 0) {
            throw new IllegalArgumentException(
                    "chaseDistance cannot be negative: " + chaseDistance);
        }
        this.lockedTargetId = targetId;
        this.remainingChaseDistance = chaseDistance;
    }

    public void releaseLock() {
        lockedTargetId = null;
        remainingChaseDistance = 0;
    }

    public void reduceChaseDistance(int cellsMoved) {
        if (cellsMoved < 0) {
            throw new IllegalArgumentException("cellsMoved cannot be negative: " + cellsMoved);
        }
        remainingChaseDistance = Math.max(0, remainingChaseDistance - cellsMoved);
    }
}
