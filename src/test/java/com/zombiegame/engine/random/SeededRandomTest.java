package com.zombiegame.engine.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SeededRandomTest {

    @Test
    void sameSeedProducesTheSameSequence() {
        SeededRandom first = new SeededRandom(42L);
        SeededRandom second = new SeededRandom(42L);

        int[] firstSequence = IntStream.range(0, 100).map(i -> first.nextInt(1000)).toArray();
        int[] secondSequence = IntStream.range(0, 100).map(i -> second.nextInt(1000)).toArray();

        assertThat(firstSequence).isEqualTo(secondSequence);
    }

    @Test
    void differentSeedsProduceDifferentSequences() {
        SeededRandom first = new SeededRandom(1L);
        SeededRandom second = new SeededRandom(2L);

        int[] firstSequence = IntStream.range(0, 100).map(i -> first.nextInt(1000)).toArray();
        int[] secondSequence = IntStream.range(0, 100).map(i -> second.nextInt(1000)).toArray();

        assertThat(firstSequence).isNotEqualTo(secondSequence);
    }

    @Test
    void nextIntStaysWithinBound() {
        SeededRandom random = new SeededRandom(7L);

        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextInt(10)).isBetween(0, 9);
        }
    }

    @Test
    void nextDoubleStaysWithinUnitRange() {
        SeededRandom random = new SeededRandom(7L);

        for (int i = 0; i < 1000; i++) {
            assertThat(random.nextDouble()).isBetween(0.0, 1.0);
        }
    }

    @Test
    void splittingInTheSameOrderFromTheSameSeedIsDeterministic() {
        SeededRandom firstParent = new SeededRandom(99L);
        SeededRandom secondParent = new SeededRandom(99L);

        int[] firstChildSequence = drawSequence(firstParent.split());
        int[] secondChildSequence = drawSequence(secondParent.split());

        assertThat(firstChildSequence).isEqualTo(secondChildSequence);
    }

    @Test
    void splitProducesAnIndependentStreamFromItsParent() {
        SeededRandom parent = new SeededRandom(99L);
        SeededRandom child = parent.split();

        int[] parentSequence = drawSequence(parent);
        int[] childSequence = drawSequence(child);

        assertThat(parentSequence).isNotEqualTo(childSequence);
    }

    @Test
    void successiveSplitsFromTheSameParentDiffer() {
        SeededRandom parent = new SeededRandom(99L);

        int[] firstChildSequence = drawSequence(parent.split());
        int[] secondChildSequence = drawSequence(parent.split());

        assertThat(firstChildSequence).isNotEqualTo(secondChildSequence);
    }

    private static int[] drawSequence(SeededRandom random) {
        return IntStream.range(0, 100).map(i -> random.nextInt(1000)).toArray();
    }
}
