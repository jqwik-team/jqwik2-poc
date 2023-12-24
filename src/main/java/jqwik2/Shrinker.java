package jqwik2;

import java.util.*;
import java.util.function.*;

public class Shrinker {
	private final Function<List<Object>, ExecutionResult> property;
	private Sample best;

	public Shrinker(Sample sample, Function<List<Object>, ExecutionResult> property) {
		this.property = property;
		best = sample;
	}

	public Sample best() {
		return best;
	}

	@SuppressWarnings("OverlyLongMethod")
	public Optional<Sample> next() {
		Set<Sample> triedFilteredSamples = new HashSet<>();
		SortedSet<Sample> filteredSamples = new TreeSet<>();
		Sample shrinkBase = best;
		while (true) {
			// System.out.println("shrinkBase: " + shrinkBase);
			Optional<Sample> shrinkingResult =
				shrinkBase.shrink()
						  .map(sample -> {
							  ExecutionResult executionResult = property.apply(sample.values());
							  return new Pair<>(sample, executionResult);
						  })
						  .filter(pair -> pair.second() != ExecutionResult.SUCCESSFUL)
						  .filter(pair -> pair.first().compareTo(best) < 0)
						  .peek(pair -> {
							  if (pair.second() == ExecutionResult.ABORTED) {
								  filteredSamples.add(pair.first());
							  }
						  })
						  .filter(pair -> pair.second() == ExecutionResult.FAILED)
						  .map(Pair::first)
						  .findAny();

			if (shrinkingResult.isPresent()) {
				best = shrinkingResult.get();
				return Optional.of(best);
			}

			if (filteredSamples.isEmpty()) {
				return Optional.empty();
			}

			while(!filteredSamples.isEmpty()) {
				shrinkBase = filteredSamples.removeFirst();
				if (triedFilteredSamples.contains(shrinkBase)) {
					continue;
				}
				triedFilteredSamples.add(shrinkBase);
			}

		}
	}

}
