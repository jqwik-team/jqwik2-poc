package jqwik2;

import java.util.*;
import java.util.random.*;

public class RandomSource implements GenerationSource {

	final RandomGenerator random;

	public RandomSource() {
		this(new Random().nextLong());
	}

	public RandomSource(long seed) {
		this.random = new Random(seed); // To make performance comparable to jqwik1
		// this.random = new SplittableRandom(seed);
	}

	@Override
	public int[] next(int count, int min, int max) {
		int bound = max == Integer.MAX_VALUE ? Integer.MAX_VALUE : max + 1;
		return random.ints(count, min, bound).limit(count).toArray();
	}
}
