package jqwik2;

import java.util.*;

public abstract class RandomChoice {

	/**
	 * Return a random value, equally distributes between 0 and max - 1.
	 *
	 * @param max The max choice to return
	 * @return A random value between 0 and max - 1
	 */
	public abstract int nextInt(int max);

	/**
	 * Create a new source of randomness that is independent of this one,
	 * i.e. it does not share any state with this one.
	 *
	 * @return new source of randomness
	 */
	public abstract RandomChoice split();

	public static RandomChoice create() {
		return new XORShiftRandomChoice();
	}

	public static RandomChoice create(String seed) {
		try {
			return new XORShiftRandomChoice(seed);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(String.format("[%s] is not a valid random randomSeed.", seed));
		}
	}

	private static class XORShiftRandomChoice extends RandomChoice {
		private final Random random;

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
		public int nextInt(int max) {
			return random.nextInt(max);
		}

		@Override
		public RandomChoice split() {
			return new XORShiftRandomChoice(new XORShiftRandom(random.nextLong()));
		}
	}

	/**
	 * A faster but not thread safe implementation of {@linkplain Random}.
	 * It also has a period of 2^n - 1 and better statistical randomness.
	 * <p>
	 * See for details: https://www.javamex.com/tutorials/random_numbers/xorshift.shtml
	 *
	 * <p>
	 * For further performance improvements within jqwik, consider to override:
	 * <ul>
	 *     <li>nextDouble()</li>
	 *     <li>nextBytes(int)</li>
	 * </ul>
	 */
	static class XORShiftRandom extends Random {
		private long seed;

		XORShiftRandom() {
			this(System.nanoTime());
		}

		private XORShiftRandom(long seed) {
			this.seed = mix64(seed);
			if (this.seed == 0) {
				// 0 is invalid for XorShift randomSeed, so we set it to a non-zero value
				this.seed = 0xbf58476d1ce4e5b9L;
			}
		}

		/**
		 * See <a href="http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html">Better Bit Mixing - Improving on MurmurHash3's 64-bit Finalizer</a>
		 */
		private static long mix64(long z) {
			z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
			z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
			return z ^ (z >>> 31);
		}

		@Override
		protected int next(int nbits) {
			long x = nextLong();
			x &= ((1L << nbits) - 1);
			return (int) x;
		}

		/**
		 * Will never generate 0L
		 */
		@Override
		public long nextLong() {
			long x = this.seed;
			x ^= (x << 21);
			x ^= (x >>> 35);
			x ^= (x << 4);
			this.seed = x;
			return x;
		}
	}
}
