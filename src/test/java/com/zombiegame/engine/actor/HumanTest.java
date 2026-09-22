package com.zombiegame.engine.actor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zombiegame.engine.grid.Coordinate;
import com.zombiegame.engine.resource.ResourceNode;
import org.junit.jupiter.api.Test;

class HumanTest {

    @Test
    void startsAliveAtItsSpawnPosition() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 3));

        assertThat(scientist.id()).isEqualTo(1);
        assertThat(scientist.position()).isEqualTo(new Coordinate(0, 3));
        assertThat(scientist.isAlive()).isTrue();
        assertThat(scientist.isCarryingResource()).isFalse();
    }

    @Test
    void movesToNewPosition() {
        PoliceOfficer officer = new PoliceOfficer(2, new Coordinate(19, 0));

        officer.moveTo(new Coordinate(18, 1));

        assertThat(officer.position()).isEqualTo(new Coordinate(18, 1));
    }

    @Test
    void canBeKilled() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));

        scientist.kill();

        assertThat(scientist.isAlive()).isFalse();
    }

    @Test
    void rejectsANullSpawnPosition() {
        assertThatThrownBy(() -> new Scientist(1, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsMovingToANullPosition() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));

        assertThatThrownBy(() -> scientist.moveTo(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsMovingADeadActor() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));
        scientist.kill();

        assertThatThrownBy(() -> scientist.moveTo(new Coordinate(1, 1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsPickingUpAfterDeath() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));
        scientist.kill();

        assertThatThrownBy(() -> scientist.pickUp(new ResourceNode(1)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsDroppingAfterDeath() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));
        scientist.pickUp(new ResourceNode(1));
        scientist.kill();

        assertThatThrownBy(scientist::dropCarriedResource)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void picksUpAndDropsResource() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));
        ResourceNode resource = new ResourceNode(7);

        scientist.pickUp(resource);

        assertThat(scientist.isCarryingResource()).isTrue();
        assertThat(scientist.carriedResource()).isEqualTo(resource);
        assertThat(scientist.dropCarriedResource()).isEqualTo(resource);
        assertThat(scientist.isCarryingResource()).isFalse();
    }

    @Test
    void rejectsPickingUpNullResource() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));

        assertThatThrownBy(() -> scientist.pickUp(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsPickingUpWhileAlreadyCarrying() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));
        scientist.pickUp(new ResourceNode(1));

        assertThatThrownBy(() -> scientist.pickUp(new ResourceNode(2)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsDroppingWhenNotCarrying() {
        Scientist scientist = new Scientist(1, new Coordinate(0, 0));

        assertThatThrownBy(scientist::dropCarriedResource)
                .isInstanceOf(IllegalStateException.class);
    }
}
