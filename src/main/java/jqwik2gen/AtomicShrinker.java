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
		addCandidatesFromRight(candidates);
		addCandidatesFromLeft(candidates);
		return candidates.stream();
	}

	private void addCandidatesFromRight(Set<AtomicRecording> candidates) {
		List<Integer> last = seeds;
		for (int i = seeds.size() - 1; i >= 0; i--) {
			while (true) {
				List<Integer> shrunk = new ArrayList<>(last);
				int seedValue = shrunk.get(i);
				if (seedValue == 0) {
					break;
				}
				shrunk.set(i, shrinkValue(seedValue));
				candidates.add(new AtomicRecording(shrunk));
				last = shrunk;
			}
		}
	}

	private void addCandidatesFromLeft(Set<AtomicRecording> candidates) {
		List<Integer> last = seeds;
		for (int i = 0; i < seeds.size(); i++) {
			while (true) {
				List<Integer> shrunk = new ArrayList<>(last);
				int seedValue = shrunk.get(i);
				if (seedValue == 0) {
					break;
				}
				shrunk.set(i, shrinkValue(seedValue));
				candidates.add(new AtomicRecording(shrunk));
				last = shrunk;
			}
		}
	}

	private int shrinkValue(int seedValue) {
		if (seedValue <= 5) {
			return --seedValue;
		}
		return seedValue / 2;
	}

}
