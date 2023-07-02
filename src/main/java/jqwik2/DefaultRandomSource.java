package jqwik2;

import java.util.*;
import java.util.random.*;

public class DefaultRandomSource implements RandomSource {

	final RandomGenerator random;


	public DefaultRandomSource() {
		this(new Random().nextLong());
	}

	public DefaultRandomSource(long seed) {
		this.random = new SplittableRandom(seed);
	}

	@Override
	public int[] next(int count) {
		return random.ints(count).limit(count).toArray();
	}
}
