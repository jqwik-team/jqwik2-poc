package jqwik2.api;

import java.util.random.*;

import jqwik2.internal.*;

/**
 * Implementations of this interface are used to generate random values.
 *
 * <p>The implementation must not be thread-safe since sample generation
 * is performed in a single thread</p>
 */
public interface RandomChoice {

	interface Distribution {
		int nextInt(RandomChoice random, int maxExcluded);
	}

	/**
	 * Return a random value, equally distributes between 0 and maxExcluded - 1.
	 *
	 * @param maxExcluded The max choice to return
	 * @return A random value between 0 and max - 1
	 */
	int nextInt(int maxExcluded);

	/**
	 * Return a random value between 0 and maxExcluded - 1.
	 *
	 * @param maxExcluded The max choice to return
	 * @param distribution The random distribution to use
	 * @return A random value between 0 and max - 1
	 */
	default int nextInt(int maxExcluded, Distribution distribution) {
		return distribution.nextInt(this, maxExcluded);
	}

	/**
	 * Create a new source of randomness that is independent of this one,
	 * i.e. it does not share any state with this one.
	 *
	 * @return new source of randomness
	 */
	RandomChoice split();

	static RandomChoice create() {
		return new XORShiftRandomChoice();
	}

	static RandomChoice create(String seed) {
		try {
			return new XORShiftRandomChoice(seed);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(String.format("[%s] is not a valid random randomSeed.", seed));
		}
	}

	/**
	 * Returns a pseudorandom {@code double} value between zero (inclusive) and
	 * one (exclusive).
	 */
	double nextDouble();

	/**
	 * Returns a {@code double} value pseudo-randomly chosen from a Gaussian
	 * (normal) distribution whose mean is 0 and whose standard deviation is 1.
	 */
	double nextGaussian();

	class XORShiftRandomChoice implements RandomChoice {
		private final RandomGenerator random;

		private XORShiftRandomChoice(String seed) {
			try {
				this.random = new XORShiftRandom(Long.parseLong(seed));
			} catch (NumberFormatException nfe) {
				String message = String.format("[%s] is not a valid random randomSeed.", seed);
				throw new IllegalArgumentException(message);
			}
		}

		private XORShiftRandomChoice() {
			this(new XORShiftRandom());
		}

		private XORShiftRandomChoice(XORShiftRandom random) {
			this.random = random;
		}

		@Override
		public int nextInt(int maxExcluded) {
			return random.nextInt(maxExcluded);
		}

		@Override
		public RandomChoice split() {
			return new XORShiftRandomChoice(new XORShiftRandom(random.nextLong()));
		}

		@Override
		public double nextDouble() {
			return random.nextDouble();
		}

		@Override
		public double nextGaussian() {
			return random.nextGaussian();
		}
	}

}
