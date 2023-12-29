package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.ExhaustiveGenerator;
import jqwik2.api.*;
import jqwik2.api.recording.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ExhaustiveGenerationTests {

	@Example
	void smallIntegers() {
		Generator<Integer> ints0to10 = new IntegerGenerator(0, 10);

		ExhaustiveGenerator exhaustive = ints0to10.exhaustive();
		assertThat(exhaustive.maxCount()).hasValue(11L);

		List<Integer> allValues = collectAllEdgeCases(exhaustive, ints0to10);
		assertThat(allValues).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

	}

	private static <T> List<T> collectAllEdgeCases(Iterable<Recording> recordings, Generator<T> generator) {
		return StreamSupport.stream(recordings.spliterator(), false)
							.map(RecordedSource::new)
							.map(generator::generate)
							.collect(Collectors.toList());
	}

}
