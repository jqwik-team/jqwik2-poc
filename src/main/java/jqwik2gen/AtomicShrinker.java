package jqwik2gen;

import java.util.*;
import java.util.stream.*;

class AtomicShrinker {

	private final List<Integer> seeds;

	AtomicShrinker(AtomicRecording recording) {
		this.seeds = recording.seeds();
	}

	Stream<AtomicRecording> shrink() {
		Set<AtomicRecording> candidates = new LinkedHashSet<>();
		for (int i = 0; i < seeds.size(); i++) {
			int current = seeds.get(i);
			for (Integer integer : shrinkValue(current)) {
				List<Integer> shrunk = new ArrayList<>(seeds);
				shrunk.set(i, integer);
				candidates.add(new AtomicRecording(shrunk));
			}
		}
		return candidates.stream();
	}

	private Set<Integer> shrinkValue(int seedValue) {
		if (seedValue == 0) {
			return Set.of();
		}
		Set<Integer> shrunkValues = new LinkedHashSet<>();
		shrunkValues.add(0);
		if (seedValue > 1) {
			shrunkValues.add(1);
		}
		shrunkValues.add(seedValue - 1);
		shrunkValues.add(seedValue / 2);
		return shrunkValues;
	}

}
