package com.zombiegame.engine.actor;

import com.zombiegame.engine.grid.Coordinate;
import java.util.Objects;

public abstract sealed class Actor permits Human, Zombie {

    private final int id;
    private Coordinate position;
    private boolean alive = true;

    protected Actor(int id, Coordinate position) {
        this.id = id;
        this.position = Objects.requireNonNull(position, "position");
    }

    public int id() {
        return id;
    }

    public Coordinate position() {
        return position;
    }

    public void moveTo(Coordinate newPosition) {
        Objects.requireNonNull(newPosition, "newPosition");
        requireAlive();
        this.position = newPosition;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }

    protected void requireAlive() {
        if (!alive) {
            throw new IllegalStateException("Actor " + id + " is dead");
        }
    }
}
