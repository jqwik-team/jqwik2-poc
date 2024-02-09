package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class FrequencyBasedDistribution implements RandomChoice.Distribution {

	private final List<Integer> ranges;
	private final int maxRange;

	<T> FrequencyBasedDistribution(List<Pair<Integer, T>> frequencies) {
		List<Integer> weights = frequencies.stream().map(Pair::first).toList();
		this.ranges = calculateRanges(weights);
		this.maxRange = ranges.getLast() + 1;
	}

	@Override
	public int nextInt(RandomChoice random, int maxExcluded) {
		int range = random.nextInt(maxRange);
		for (int i = 0; i < ranges.size(); i++) {
			if (range <= ranges.get(i)) {
				return i;
			}
		}
		// Should never happen
		return 0;
	}

	private List<Integer> calculateRanges(List<Integer> weights) {
		int upper = 0;
		List<Integer> ranges = new ArrayList<>();
		for (int weight : weights) {
			upper += weight;
			ranges.add(upper);
		}
		return ranges;
	}

}
