package com.zombiegame.engine.actor;

import com.zombiegame.engine.grid.Coordinate;
import com.zombiegame.engine.resource.ResourceNode;
import java.util.Objects;

public abstract sealed class Human extends Actor permits Scientist, PoliceOfficer {

    private ResourceNode carriedResource;

    protected Human(int id, Coordinate position) {
        super(id, position);
    }

    public boolean isCarryingResource() {
        return carriedResource != null;
    }

    public ResourceNode carriedResource() {
        return carriedResource;
    }

    public void pickUp(ResourceNode resource) {
        Objects.requireNonNull(resource, "resource");
        requireAlive();
        if (carriedResource != null) {
            throw new IllegalStateException("Already carrying a resource: " + carriedResource);
        }
        carriedResource = resource;
    }

    public ResourceNode dropCarriedResource() {
        requireAlive();
        if (carriedResource == null) {
            throw new IllegalStateException("Not carrying a resource");
        }
        ResourceNode dropped = carriedResource;
        carriedResource = null;
        return dropped;
    }
}
