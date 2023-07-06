package jqwik2;

import java.util.*;
import java.util.random.*;

public class RandomSource implements GenerationSource {

	final RandomGenerator random;

	public RandomSource() {
		this(new Random().nextLong());
	}

	public RandomSource(long seed) {
		this.random = new SplittableRandom(seed);
	}

	@Override
	public int[] next(int count, int min, int max) {
		int bound = Math.min(max + 1, Integer.MAX_VALUE); // Warning is wrong because int overflow possible
		return random.ints(count, min, bound).limit(count).toArray();
	}
}
