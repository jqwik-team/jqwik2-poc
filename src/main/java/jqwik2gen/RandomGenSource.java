package jqwik2gen;

import java.util.*;

public final class RandomGenSource implements GenSource {
	private final Random random;

	public RandomGenSource() {
		this.random = new Random();
	}

	public RandomGenSource(long seed) {
		this.random = new Random(seed);
	}

	@Override
	public int next(int max) {
		return random.nextInt(max);
	}

	@Override
	public GenSource child() {
		return new RandomGenSource(random.nextLong());
	}

}
