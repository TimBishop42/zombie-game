package com.zombiegame.engine.grid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zombiegame.engine.resource.ResourceNode;
import org.junit.jupiter.api.Test;

class GridTest {

    @Test
    void rejectsNonPositiveSize() {
        assertThatThrownBy(() -> new Grid(0)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void tracksBounds() {
        Grid grid = new Grid(5);

        assertThat(grid.isInBounds(new Coordinate(0, 0))).isTrue();
        assertThat(grid.isInBounds(new Coordinate(4, 4))).isTrue();
        assertThat(grid.isInBounds(new Coordinate(5, 0))).isFalse();
        assertThat(grid.isInBounds(new Coordinate(0, -1))).isFalse();
    }

    @Test
    void placesAndRetrievesResource() {
        Grid grid = new Grid(5);
        ResourceNode resource = new ResourceNode(1);
        Coordinate coordinate = new Coordinate(2, 2);

        grid.placeResource(coordinate, resource);

        assertThat(grid.resourceAt(coordinate)).contains(resource);
        assertThat(grid.remainingResourceCount()).isEqualTo(1);
    }

    @Test
    void rejectsPlacingOutOfBounds() {
        Grid grid = new Grid(5);

        assertThatThrownBy(() -> grid.placeResource(new Coordinate(5, 5), new ResourceNode(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsPlacingNullResource() {
        Grid grid = new Grid(5);

        assertThatThrownBy(() -> grid.placeResource(new Coordinate(0, 0), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsPlacingOnAnOccupiedCell() {
        Grid grid = new Grid(5);
        Coordinate coordinate = new Coordinate(1, 1);
        grid.placeResource(coordinate, new ResourceNode(1));

        assertThatThrownBy(() -> grid.placeResource(coordinate, new ResourceNode(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void removesResource() {
        Grid grid = new Grid(5);
        Coordinate coordinate = new Coordinate(1, 1);
        grid.placeResource(coordinate, new ResourceNode(1));

        grid.removeResource(coordinate);

        assertThat(grid.resourceAt(coordinate)).isEmpty();
        assertThat(grid.remainingResourceCount()).isZero();
    }

    @Test
    void rejectsRemovingFromAnEmptyCell() {
        Grid grid = new Grid(5);

        assertThatThrownBy(() -> grid.removeResource(new Coordinate(1, 1)))
                .isInstanceOf(IllegalStateException.class);
    }
}
