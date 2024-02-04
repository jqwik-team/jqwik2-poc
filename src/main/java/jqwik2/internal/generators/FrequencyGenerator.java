package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;
import jqwik2.internal.*;

public class FrequencyGenerator<T> implements Generator<T> {

	private final List<Pair<Integer, T>> frequencies;
	private final List<T> values;
	private final RandomChoice.Distribution distribution;

	public FrequencyGenerator(Collection<Pair<Integer, T>> frequencies) {
		this.values = values(frequencies);
		this.frequencies = frequencies.stream()
									  .filter(p -> p.first() > 0)
									  .map(p -> new Pair<>(p.first(), (T) p.second()))
									  .toList();
		this.distribution = frequencyDistribution();
	}

	private List<T> values(Collection<Pair<Integer, T>> frequencies) {
		return frequencies.stream()
						  .filter(p -> p.first() > 0)
						  .map(Pair::second)
						  .distinct()
						  .map(v -> (T) v)
						  .toList();
	}

	@Override
	public T generate(GenSource source) {
		if (frequencies.isEmpty()) {
			throw new CannotGenerateException("No values to choose from");
		}
		int index = source.choice().choose(frequencies.size(), distribution);
		return frequencies.get(index).second();
	}

	private RandomChoice.Distribution frequencyDistribution() {
		return new FrequencyBasedDistribution();
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return EdgeCasesSupport.forChoice(values.size() - 1);
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.choice(values.size() - 1);
	}

	private class FrequencyBasedDistribution implements RandomChoice.Distribution {

		private final List<Integer> ranges;
		private final int maxRange;

		private FrequencyBasedDistribution() {
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

		private java.util.List<Integer> calculateRanges(List<Integer> weights) {
			int upper = 0;
			List<Integer> ranges = new ArrayList<>();
			for (int weight : weights) {
				upper += weight;
				ranges.add(upper);
			}
			return ranges;
		}

	}

}
