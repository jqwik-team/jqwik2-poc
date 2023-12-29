package jqwik2.api.support;

import java.util.*;
import java.util.stream.*;

import jqwik2.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveGenerationSupport {
	public static ExhaustiveGenerator forAtom(Integer... maxChoicesIncluded) {
		if (maxChoicesIncluded.length != 1) {
			throw new IllegalArgumentException("Exhaustive generation only supported for single value generators");
		}

		long maxCount = Arrays.stream(maxChoicesIncluded)
							  .mapToLong(i -> i + 1)
							  .reduce(1, (a, b) -> a * b);

		Iterable<Recording> recordings = IntStream.range(0, maxChoicesIncluded[0] + 1)
												  .mapToObj(Recording::atom)
												  .collect(Collectors.toList());

		return new RecordingBasedExhaustiveGenerator(recordings, maxCount);
	}
}
