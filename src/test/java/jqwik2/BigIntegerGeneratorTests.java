package jqwik2;

import java.math.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class BigIntegerGeneratorTests {

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
