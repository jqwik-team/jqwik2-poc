package jqwik2;

import java.util.*;
import java.util.stream.*;

class AtomShrinker {

	private final List<Integer> seeds;

	AtomShrinker(AtomRecording recording) {
		this.seeds = recording.choices();
	}

	Stream<AtomRecording> shrink() {
		Set<AtomRecording> candidates = new LinkedHashSet<>();
		for (int i = 0; i < seeds.size(); i++) {
			int current = seeds.get(i);
			for (Integer integer : shrinkValue(current)) {
				List<Integer> shrunk = new ArrayList<>(seeds);
				shrunk.set(i, integer);
				candidates.add(new AtomRecording(shrunk));
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
