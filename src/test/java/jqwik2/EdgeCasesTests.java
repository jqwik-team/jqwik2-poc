package jqwik2;

import java.util.*;

import net.jqwik.api.*;

import static jqwik2.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

class EdgeCasesTests {

	@Example
	void intEdgeCasesCanBeGenerated() {
		Generator<Integer> allInts = new IntegerGenerator(Integer.MIN_VALUE, Integer.MAX_VALUE);

		GenSource maxValueSource = new RecordedSource(atom(Integer.MAX_VALUE, 0));
		GenSource minValueSource = new RecordedSource(atom(Integer.MAX_VALUE, 2));

		assertThat(allInts.generate(maxValueSource)).isEqualTo(Integer.MAX_VALUE);
		assertThat(allInts.generate(minValueSource)).isEqualTo(Integer.MIN_VALUE);

		IntegerGenerator positiveInts = new IntegerGenerator(0, Integer.MAX_VALUE);
		// RecordedSource has state and must be recreated for each test
		maxValueSource = new RecordedSource(atom(Integer.MAX_VALUE, 0));
		assertThat(positiveInts.generate(maxValueSource)).isEqualTo(Integer.MAX_VALUE);

		IntegerGenerator negativeInts = new IntegerGenerator(Integer.MIN_VALUE, 0);
		// RecordedSource has state and must be recreated for each test
		minValueSource = new RecordedSource(atom(Integer.MAX_VALUE, 2));
		assertThat(negativeInts.generate(minValueSource)).isEqualTo(Integer.MIN_VALUE);
	}

	@Example
	void integerEdgeCases() {
		Generator<Integer> allInts = new IntegerGenerator(Integer.MIN_VALUE, Integer.MAX_VALUE);

		Set<Integer> generatedEdgeCases = getGeneratedEdgeCases(allInts);

		assertThat(generatedEdgeCases).containsExactly(
			Integer.MIN_VALUE,
			-1,
			0,
			1,
			Integer.MAX_VALUE
		);

	}

	private static <T> Set<T> getGeneratedEdgeCases(Generator<T> generator) {
		GenSource edgeCasesSource = generator.edgeCases();
		Set<T> generatedEdgeCases = new LinkedHashSet<>();
		while (true) {
			try {
				T value = generator.generate(edgeCasesSource);
				generatedEdgeCases.add(value);
			} catch (CannotGenerateException e) {
				break;
			}
		}
		return generatedEdgeCases;
	}

}
