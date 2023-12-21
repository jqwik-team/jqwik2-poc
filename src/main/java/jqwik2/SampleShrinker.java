package jqwik2;

import java.util.*;
import java.util.stream.*;

class SampleShrinker {

	private final List<Shrinkable<Object>> shrinkables;

	SampleShrinker(Sample sample) {
		this.shrinkables = sample.shrinkables();
	}

	Stream<Sample> shrink() {
		if (shrinkables.size() != 1) {
			throw new IllegalArgumentException("Only one parameter supported for now!");
		}
		Shrinkable<Object> firstParam = shrinkables.getFirst();
		return firstParam.shrink().map((Shrinkable<Object> s) -> new Sample(List.of(s)));
	}
}
