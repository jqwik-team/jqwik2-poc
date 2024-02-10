package jqwik2;

import java.util.*;
import java.util.function.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.*;
import jqwik2.api.arbitraries.Combinators;
import jqwik2.api.arbitraries.*;
import jqwik2.internal.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ArbitrariesTests {

	@Example
	void integers() {
		for (int i = 0; i < 10; i++) {
			Arbitrary<Integer> ints = Numbers.integers();
			int sample = ints.sample();
			// System.out.println(sample);
			assertThat(sample).isBetween(Integer.MIN_VALUE, Integer.MAX_VALUE);
		}
	}

	@Example
	void just() {
		Arbitrary<Integer> c42 = Values.just(42);
		Arbitrary<String> cHallo = Values.just("hallo");
		assertThat(c42.sample()).isEqualTo(42);
		assertThat(cHallo.sample()).isEqualTo("hallo");

		assertThat(c42).isEqualTo(Values.just(42));
		assertThat(c42).isNotEqualTo(Values.just(41));
	}

	@Example
	void lists() {
		for (int i = 0; i < 10; i++) {
			Arbitrary<List<Integer>> ints = Numbers.integers().list().ofSize(5);
			List<Integer> sample = ints.sample();
			// System.out.println(sample);
			assertThat(sample).hasSize(5);
		}
	}

	@Example
	void sets() {
		for (int i = 0; i < 10; i++) {
			Arbitrary<Set<Integer>> ints = Numbers.integers().set().ofSize(5);
			Set<Integer> sample = ints.sample();
			// System.out.println(sample);
			assertThat(sample).hasSize(5);
		}
	}

	@Example
	void mapping() {
		for (int i = 0; i < 10; i++) {
			Arbitrary<String> hexInts = Numbers.integers().between(0, 10000)
											   .map(anInt -> Integer.toHexString(anInt));
			String sample = hexInts.sample();
			// System.out.println(sample);
			assertThat(Integer.valueOf(sample, 16)).isBetween(0, 10000);
		}
	}

	@Example
	void flatMapping() {
		Arbitrary<List<Integer>> list = Numbers.integers().between(1, 5)
											   .flatMap(anInt -> Values.just(anInt).list().ofSize(anInt));
		for (int i = 0; i < 10; i++) {
			List<Integer> sample = list.sample();
			// System.out.println(sample);
			assertThat(sample).hasSizeBetween(1, 5);
			assertThat(sample).containsOnly(sample.size());
		}
	}

	@Example
	void flatMappingExhaustiveGeneration() {
		Arbitrary<List<Integer>> list = Numbers.integers().between(1, 5)
											   .flatMap(anInt -> Values.just(anInt).list().ofMaxSize(anInt));

		ExhaustiveSource<?> exhaustiveSource = list.generator().exhaustive().get();
		assertThat(exhaustiveSource.maxCount()).isEqualTo(20);
		int count = 0;
		for (GenSource genSource : exhaustiveSource) {
			count++;
			// System.out.println(genSource);
		}
		assertThat(count).isEqualTo(20);
	}

	@Example
	void simpleCombinations() {
		Arbitrary<Integer> ints = Numbers.integers().between(1, 1000);
		Arbitrary<String> strings = Numbers.integers().between(1, 100).map(Object::toString);

		Function<Combinators.Sampler, Integer> combinator = sampler -> {
			int anInt = sampler.draw(ints);
			String aString = sampler.draw(strings);
			return anInt + aString.length();
		};
		Arbitrary<Integer> combined = Combinators.combine(combinator);

		combined.samples(true).limit(10).forEach(sample -> {
			// System.out.println(sample);
			assertThat(sample).isBetween(2, 1003);
		});

		assertThat(combined).isEqualTo(Combinators.combine(combinator));
		assertThat(combined).isNotEqualTo(Combinators.combine(sampler -> 42));
	}

	@Example
	void chooseValue() {
		Arbitrary<String> choices = Values.of("a", "b", "c");
		choices.samples(false).limit(10).forEach(sample -> {
			// System.out.println(sample);
			assertThat(sample).isIn("a", "b", "c");
		});

		assertThat(choices).isEqualTo(Values.of("a", "b", "c"));
		assertThat(choices).isNotEqualTo(Values.of("a", "b", null));
	}

	@Example
	void chooseValueWithFrequencies() {
		Arbitrary<String> choices = Values.frequency(
			new Pair<>(1, "a"),
			new Pair<>(5, "b"),
			new Pair<>(10, "c")
		);
		choices.samples(false).limit(10).forEach(sample -> {
			// System.out.println(sample);
			assertThat(sample).isIn("a", "b", "c");
		});

		assertThat(choices).isEqualTo(Values.frequency(
			new Pair<>(1, "a"),
			new Pair<>(5, "b"),
			new Pair<>(10, "c")
		));
		assertThat(choices).isNotEqualTo(Values.of("a", "b", "c"));
	}

	@Example
	void chooseArbitrary() {
		Arbitrary<Integer> choices = Values.oneOf(
			Numbers.integers().between(1, 5),
			Values.just(42)
		);
		choices.samples(false).limit(10).forEach(sample -> {
			// System.out.println(sample);
			assertThat(sample).isIn(1, 2, 3, 4, 5, 42);
		});

		assertThat(choices).isEqualTo(Values.oneOf(
			Numbers.integers().between(1, 5),
			Values.just(42)
		));
		assertThat(choices).isNotEqualTo(Values.oneOf(
			Numbers.integers().between(1, 5),
			Values.just(43)
		));
	}

	@Example
	void lazyArbitrary() {
		Arbitrary<Integer> choices = Values.lazy(() -> Values.of(1, 2, 3));
		choices.samples(false).limit(10).forEach(sample -> {
			// System.out.println(sample);
			assertThat(sample).isIn(1, 2, 3);
		});
	}
}
