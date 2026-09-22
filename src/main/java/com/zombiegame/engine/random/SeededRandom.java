package com.zombiegame.engine.random;

import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.random.RandomGeneratorFactory;

public final class SeededRandom {

    private final SplittableGenerator generator;

    public SeededRandom(long seed) {
        this((SplittableGenerator) RandomGeneratorFactory.of("L64X256MixRandom").create(seed));
    }

    private SeededRandom(SplittableGenerator generator) {
        this.generator = generator;
    }

    public int nextInt(int bound) {
        return generator.nextInt(bound);
    }

    public double nextDouble() {
        return generator.nextDouble();
    }

    // Not thread-safe: a shared instance must never be drawn from concurrently, since which
    // actor gets which value would then depend on thread scheduling, not the seed. Parallel
    // callers must call split() once per task, in a fixed order, before fanning out.
    public SeededRandom split() {
        return new SeededRandom(generator.split());
    }
}
