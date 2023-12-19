package jqwik2;

import java.util.*;

import org.assertj.core.api.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class SampleTests {

	@Example
	void generateRandomSample() {
		Generator<Integer> ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 5);

		Sample sample = RandomSample.create(List.of(ints, lists), "42");

		System.out.println("sample = " + sample);

		Sample recreated = sample.regenerate();
		assertThat(recreated).isEqualTo(sample);
	}
}
