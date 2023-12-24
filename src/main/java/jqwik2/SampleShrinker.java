package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.support.*;

public class SampleShrinker {

	private final List<Shrinkable<Object>> shrinkables;

	public SampleShrinker(Sample sample) {
		this.shrinkables = sample.shrinkables();
	}

	public Stream<Sample> shrink() {
		// TODO: Implement pairwise shrinking
		// TODO: Implement grow and shrink
		return shrinkOneAfterTheOther();
	}

	private Stream<Sample> shrinkOneAfterTheOther() {
		List<Stream<Sample>> shrinkPerPartStreams = new ArrayList<>();
		for (int i = 0; i < shrinkables.size(); i++) {
			int index = i;
			Shrinkable<Object> part = shrinkables.get(i);
			Stream<Sample> shrinkElement = part.shrink().flatMap(shrunkElement -> {
				List<Shrinkable<Object>> partsCopy = new ArrayList<>(shrinkables);
				partsCopy.set(index, shrunkElement);
				return Stream.of(new Sample(partsCopy));
			});
			shrinkPerPartStreams.add(shrinkElement);
		}
		return StreamConcatenation.concat(shrinkPerPartStreams);
	}

}
