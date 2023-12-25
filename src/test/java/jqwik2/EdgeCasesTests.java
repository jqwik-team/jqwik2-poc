package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.support.*;

import net.jqwik.api.*;

import static jqwik2.api.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

class EdgeCasesTests {

	@Example
	void intEdgeCasesCanBeGenerated() {
		Generator<Integer> allInts = new IntegerGenerator(Integer.MIN_VALUE, Integer.MAX_VALUE);

		GenSource maxValueSource = new RecordedSource(atom(Integer.MAX_VALUE - 1, 2));
		GenSource minValueSource = new RecordedSource(atom(Integer.MAX_VALUE - 1, 3));

		assertThat(allInts.generate(maxValueSource)).isEqualTo(Integer.MAX_VALUE);
		assertThat(allInts.generate(minValueSource)).isEqualTo(Integer.MIN_VALUE);

		IntegerGenerator positiveInts = new IntegerGenerator(0, Integer.MAX_VALUE);
		// RecordedSource has state and must be recreated for each test
		maxValueSource = new RecordedSource(atom(Integer.MAX_VALUE - 1, 2));
		assertThat(positiveInts.generate(maxValueSource)).isEqualTo(Integer.MAX_VALUE);

		IntegerGenerator negativeInts = new IntegerGenerator(Integer.MIN_VALUE, 0);
		// RecordedSource has state and must be recreated for each test
		minValueSource = new RecordedSource(atom(Integer.MAX_VALUE - 1, 3));
		assertThat(negativeInts.generate(minValueSource)).isEqualTo(Integer.MIN_VALUE);

		IntegerGenerator smallPositiveInts = new IntegerGenerator(10, Integer.MAX_VALUE);
		// RecordedSource has state and must be recreated for each test
		maxValueSource = new RecordedSource(atom(Integer.MAX_VALUE - 10));
		assertThat(smallPositiveInts.generate(maxValueSource)).isEqualTo(Integer.MAX_VALUE);

		IntegerGenerator smallNegativeInts = new IntegerGenerator(Integer.MIN_VALUE, -10);
		// RecordedSource has state and must be recreated for each test
		maxValueSource = new RecordedSource(atom(Integer.MAX_VALUE - 9));
		assertThat(smallNegativeInts.generate(maxValueSource)).isEqualTo(Integer.MIN_VALUE);
	}

	@Example
	void allIntegersEdgeCases() {
		Generator<Integer> allInts = new IntegerGenerator(Integer.MIN_VALUE, Integer.MAX_VALUE);

		Set<Integer> generatedEdgeCases = collectAllEdgeCases(allInts);

		assertThat(generatedEdgeCases).containsExactlyInAnyOrder(
			Integer.MIN_VALUE,
			-1,
			0,
			1,
			Integer.MAX_VALUE
		);
	}

	@Example
	void constrainedIntegerEdgeCases() {
		Generator<Integer> someInts = new IntegerGenerator(-10, 100);
		Set<Integer> generatedEdgeCases = collectAllEdgeCases(someInts);
		assertThat(generatedEdgeCases).containsExactlyInAnyOrder(
			-10,
			-1,
			0,
			1,
			100
		);

		generatedEdgeCases = collectAllEdgeCases(new IntegerGenerator(10, 100));
		assertThat(generatedEdgeCases).containsExactlyInAnyOrder(
			10,
			100
		);

		generatedEdgeCases = collectAllEdgeCases(new IntegerGenerator(-100, -10));
		assertThat(generatedEdgeCases).containsExactlyInAnyOrder(
			-100,
			-10
		);

	}

	@Example
	void allListEdgeCases() {
		// Edge cases = 0, 1, 10
		Generator<Integer> ints = new IntegerGenerator(0, 10);
		Generator<List<Integer>> lists = new ListGenerator<>(ints, 5);

		Set<List<Integer>> generatedEdgeCases = collectAllEdgeCases(lists);

		assertThat(generatedEdgeCases).containsExactlyInAnyOrder(
			List.of(),
			List.of(0),
			List.of(10)
		);
	}


	private static <T> Set<T> collectAllEdgeCases(Generator<T> generator) {
		Set<T> generatedEdgeCases = new LinkedHashSet<>();
		EdgeCasesSupport.sources(generator.edgeCases())
			.forEach(source -> {
				try {
					T value = generator.generate(source);
					generatedEdgeCases.add(value);
				} catch (CannotGenerateException e) {
					// ignore
				}
			});
		return generatedEdgeCases;
	}

}
