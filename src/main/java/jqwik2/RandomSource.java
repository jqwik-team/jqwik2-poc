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
	public int[] next(int count) {
		return random.ints(count).limit(count).toArray();
	}
}
