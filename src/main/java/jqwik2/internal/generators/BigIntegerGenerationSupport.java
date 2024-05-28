package jqwik2.internal.generators;

import java.math.*;

import jqwik2.api.*;

public class BigIntegerGenerationSupport {

	private final GenSource source;

	public BigIntegerGenerationSupport(GenSource source) {
		this.source = source;
	}

	/**
	 * Choose a random BigInteger between 0 and max. Both included.
	 * @param max
	 * @return a random BigInteger between 0 and max
	 */
	public BigInteger choosePositiveBigInteger(BigInteger max) {
		var numBits = max.bitLength();

		int bytes = (int) (((long) numBits + 7) / 8);
		byte[] magnitude = new byte[bytes];

		while (true) {
			var tuple = source.tuple();
			fillBits(magnitude, numBits, tuple);
			int signum = 1;
			BigInteger value = new BigInteger(signum, magnitude);
			if (value.compareTo(max) <= 0) {
				return value;
			}
		}
	}

	private void fillBits(byte[] bytes, int bits, GenSource.Tuple tuple) {
		var remainingBits = bits;
		// Optimization: Choose more than one byte at a time
		for (int i = 0; remainingBits > 0; i++) {
			short bitsToFill = remainingBits >= 8 ? 8 : (short) remainingBits;
			var maxExcluded = 2 << (bitsToFill - 1);
			byte b = (byte) tuple.nextValue().choice().choose(maxExcluded);
			bytes[i] = b;
			remainingBits -= bitsToFill;
		}
	}


}
