package jqwik2;

import java.math.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class BigIntegerGeneratorTests {

	@Example
	void experimentWithBigIntegerConstruction() {

		GenSource source = new RandomGenSource();
		var max = BigInteger.valueOf(1_000_000_000_000L);
		System.out.println("max=" + max);

		Map<Character, Integer> countDigits = new HashMap<>();
		Map<Integer, Integer> countLength = new HashMap<>();
		for (int i = 0; i < 10000; i++) {
			BigInteger value = randomBigInt(max, source);
			// var lastDigit = value.mod(BigInteger.TEN).intValue();
			// var firstDigit = value.divide(BigInteger.TEN.pow(value.toString().length() - 1)).intValue();
			// System.out.println("value=" + value);
			// iterate over all digits of value
			for (char c : value.toString().toCharArray()) {
				int count = countDigits.getOrDefault(c, 0);
				countDigits.put(c, count + 1);
			}
			// count length of value
			int length = value.toString().length();
			int count = countLength.getOrDefault(length, 0);
			countLength.put(length, count + 1);
		}
		System.out.println("count digits=" + countDigits);
		System.out.println("count length=" + countLength);
	}

	private BigInteger randomBigInt(BigInteger max, GenSource source) {
		var bits = max.bitLength();

		int bytes = (int) (((long) bits + 7) / 8);
		// System.out.println("max=" + max);
		// System.out.println("bits=" + bits);
		// System.out.println("bytes=" + bytes);

		byte[] magnitude = new byte[bytes];
		while(true) {
			// Generate random bytes and mask out any excess bits
			if (bytes > 0) {
				nextBytes(magnitude, source);
				int excessBits = 8 * bytes - bits;
				magnitude[0] &= (byte) ((1 << (8 - excessBits)) - 1);
			}
			int signum = 1;
			BigInteger value = new BigInteger(signum, magnitude);
			if (value.compareTo(max) <= 0) {
				return value;
			}
		}
	}

	private void nextBytes(byte[] bytes, GenSource source) {
		var tuple = source.tuple();
		for (int i = 0; i < bytes.length; i++) {
			byte b = (byte) tuple.nextValue().choice().choose(256);
			// System.out.println("b=" + b);
			bytes[i] = b;
		}
	}

	@Group
	class WithinIntegerRange {

		BigInteger min = BigInteger.valueOf(Integer.MIN_VALUE);
		BigInteger max = BigInteger.valueOf(Integer.MAX_VALUE);

		@Example
		void fromRandomGenSource() {
			Generator<BigInteger> fullRange = new BigIntegerGenerator(min, max);
			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				BigInteger value = fullRange.generate(source);
				assertThat(value).isBetween(min, max);
				// System.out.println("value=" + value);
			}
		}

		@Example
		void edgeCases() {
			Generator<BigInteger> fullRange = new BigIntegerGenerator(min, max);

			var edgeCases = EdgeCasesTests.collectAllEdgeCases(fullRange);
			assertThat(edgeCases).containsExactlyInAnyOrder(
				BigInteger.valueOf(0),
				BigInteger.valueOf(-1),
				BigInteger.valueOf(1),
				min,
				max
			);
		}

		@Example
		void exhaustiveGeneration() {
			var minus10 = BigInteger.valueOf(-10);
			var twenty = BigInteger.valueOf(20);
			Generator<BigInteger> minus10to20 = new BigIntegerGenerator(minus10, twenty);

			var exhaustive = minus10to20.exhaustive();
			assertThat(exhaustive).isPresent();
			assertThat(exhaustive.get().maxCount()).isEqualTo(31);

			List<BigInteger> all = ExhaustiveGenerationTests.collectAll(exhaustive.get(), minus10to20);
			assertThat(all).contains(minus10, twenty, BigInteger.ZERO);
		}

	}

	@Group
	@Disabled
	class UniformDistribution {

		@Example
		void fromRandomGenSource() {
			BigInteger min = BigInteger.valueOf(0);
			BigInteger max = BigInteger.valueOf(1_000_000_000_000L);
			Generator<BigInteger> fullRange = new BigIntegerGenerator(min, max);
			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				BigInteger value = fullRange.generate(source);
				assertThat(value).isBetween(min, max);
				// System.out.println("value=" + value);
			}
		}

	}

}
