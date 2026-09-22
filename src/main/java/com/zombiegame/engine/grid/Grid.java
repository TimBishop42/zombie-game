package com.zombiegame.engine.grid;

import com.zombiegame.engine.resource.ResourceNode;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class Grid {

    private final int size;
    private final Map<Coordinate, ResourceNode> resourceNodes = new HashMap<>();

    public Grid(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Grid size must be positive: " + size);
        }
        this.size = size;
    }

    public int size() {
        return size;
    }

    public boolean isInBounds(Coordinate coordinate) {
        return coordinate.x() >= 0 && coordinate.x() < size
                && coordinate.y() >= 0 && coordinate.y() < size;
    }

    public void placeResource(Coordinate coordinate, ResourceNode resource) {
        Objects.requireNonNull(resource, "resource");
        if (!isInBounds(coordinate)) {
            throw new IllegalArgumentException("Coordinate out of bounds: " + coordinate);
        }
        if (resourceNodes.containsKey(coordinate)) {
            throw new IllegalStateException("Cell already holds a resource: " + coordinate);
        }
        resourceNodes.put(coordinate, resource);
    }

    public Optional<ResourceNode> resourceAt(Coordinate coordinate) {
        return Optional.ofNullable(resourceNodes.get(coordinate));
    }

    public void removeResource(Coordinate coordinate) {
        if (resourceNodes.remove(coordinate) == null) {
            throw new IllegalStateException("No resource at: " + coordinate);
        }
    }

    public int remainingResourceCount() {
        return resourceNodes.size();
    }
}
