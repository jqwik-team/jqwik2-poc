package jqwik2;

import java.util.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.arbitraries.*;

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
	void constants() {
		Arbitrary<Integer> c42 = Values.just(42);
		Arbitrary<String> cHallo = Values.just("hallo");
		assertThat(c42.sample()).isEqualTo(42);
		assertThat(cHallo.sample()).isEqualTo("hallo");
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
		for (int i = 0; i < 10; i++) {
			Arbitrary<List<Integer>> list = Numbers.integers().between(1, 5)
												   .flatMap(anInt -> Values.just(anInt).list().ofSize(anInt));
			List<Integer> sample = list.sample();
			//System.out.println(sample);
			assertThat(sample).hasSizeBetween(1, 5);
			assertThat(sample).containsOnly(sample.size());
		}
	}

}
