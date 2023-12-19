package jqwik2;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

public class SourceOfRandomness {

	private SourceOfRandomness() {
	}

	public static Random create(String seed) {
		try {
			return newRandom(Long.parseLong(seed));
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(String.format("[%s] is not a valid random randomSeed.", seed));
		}
	}

	public static Random newRandom() {
		return new XORShiftRandom();
	}

	public static Random newRandom(final long seed) {
		return new XORShiftRandom(seed);
	}

	/**
	 * A faster but not thread safe implementation of {@linkplain Random}.
	 * It also has a period of 2^n - 1 and better statistical randomness.
	 *
	 * See for details: https://www.javamex.com/tutorials/random_numbers/xorshift.shtml
	 *
	 * <p>
	 * For further performance improvements within jqwik, consider to override:
	 * <ul>
	 *     <li>nextDouble()</li>
	 *     <li>nextBytes(int)</li>
	 * </ul>
	 */
	private static class XORShiftRandom extends Random {
		private long seed;

		private XORShiftRandom() {
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
