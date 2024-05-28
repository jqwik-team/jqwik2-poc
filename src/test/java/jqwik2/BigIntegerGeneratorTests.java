package jqwik2;

import java.math.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.recording.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class BigIntegerGeneratorTests {

	@Group
	class UniformPositiveBigIntegers {

		// @Example
		void statisticalDistributionOfGeneratedBigDecimals() {

			GenSource source = new RandomGenSource();
			var max = BigInteger.valueOf(1_000_000_000_000L);
			System.out.println("max=" + max);

			Map<Character, Integer> countLastDigits = new HashMap<>();
			Map<Character, Integer> countDigits = new HashMap<>();
			Map<Integer, Integer> countLength = new HashMap<>();

			for (int i = 0; i < 10000; i++) {
				BigInteger value = randomBigInt(max, source);

				// count all digits
				for (char c : value.toString().toCharArray()) {
					int count = countDigits.getOrDefault(c, 0);
					countDigits.put(c, count + 1);
				}

				// count last digit
				var lastDigit = value.mod(BigInteger.TEN).intValue();
				int countLD = countLastDigits.getOrDefault((char) ('0' + lastDigit), 0);
				countLastDigits.put((char) ('0' + lastDigit), countLD + 1);

				// count length of value
				int length = value.toString().length();
				int count = countLength.getOrDefault(length, 0);
				countLength.put(length, count + 1);
			}
			System.out.println("count last digits=" + countLastDigits);
			System.out.println("count digits=" + countDigits);
			System.out.println("count length=" + countLength);
		}

		@Example
		void generateSmallBigDecimals() {

			GenSource source = new RandomGenSource();
			var max = BigInteger.valueOf(10);

			for (int i = 0; i < 10; i++) {
				var recorder = new GenRecorder(source);

				BigInteger value = randomBigInt(max, recorder);

				// System.out.println("value=" + value);

				// assert that value can be regenerated
				var regenerated = randomBigInt(max, RecordedSource.of(recorder.recording()));
				assertThat(value).isEqualTo(regenerated);
			}
		}

		@Example
		void shrinkBigDecimals() {

			for (int i = 0; i < 10; i++) {
				GenRecorder recorder = new GenRecorder(new RandomGenSource());
				BigInteger value = randomBigInt(BigInteger.valueOf(1_000_000_000_000L), recorder);
				// System.out.println("value=" + value);

				recorder.recording().shrink().forEach(shrunk -> {
					// System.out.println(shrunk);
					var x = randomBigInt(BigInteger.valueOf(1_000_000_000_000L), RecordedSource.of(shrunk));
					assertThat(x).isLessThan(value);
					// System.out.println(x);
				});
			}
		}

		private BigInteger randomBigInt(BigInteger max, GenSource source) {
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
			for (int i = 0; remainingBits > 0; i++) {
				short bitsToFill = remainingBits >= 8 ? 8 : (short) remainingBits;
				var maxExcluded = 2 << (bitsToFill - 1);
				byte b = (byte) tuple.nextValue().choice().choose(maxExcluded);
				bytes[i] = b;
				remainingBits -= bitsToFill;
			}
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
