package jqwik2gen;

import java.util.*;

public final class RandomSource implements GenSource {
	private final Random random;

	public RandomSource() {
		this.random = new Random();
	}

	public RandomSource(long seed) {
		this.random = new Random(seed);
	}

	@Override
	public int next(int max) {
		return random.nextInt(max);
	}
}
