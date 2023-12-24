package jqwik2;

import java.util.*;

import jqwik2.api.*;

public class Shrinker {
	private final Tryable tryable;
	private Sample best;

	public Shrinker(Sample sample, Tryable tryable) {
		this.tryable = tryable;
		best = sample;
	}

	public Sample best() {
		return best;
	}

	@SuppressWarnings("OverlyLongMethod")
	public Optional<Sample> next() {
		Set<Sample> triedFilteredSamples = new HashSet<>();
		SortedSet<Sample> invalidSamples = new TreeSet<>();
		Sample shrinkBase = best;
		while (true) {
			// System.out.println("shrinkBase: " + shrinkBase);
			Optional<Sample> shrinkingResult =
				shrinkBase.shrink()
						  .map(sample -> {
							  TryExecutionResult executionResult = tryable.apply(sample.values());
							  return new Pair<>(sample, executionResult);
						  })
						  .filter(pair -> pair.second().status() != TryExecutionResult.Status.SATISFIED)
						  .filter(pair -> pair.first().compareTo(best) < 0)
						  .peek(pair -> {
							  if (pair.second().status() == TryExecutionResult.Status.INVALID) {
								  invalidSamples.add(pair.first());
							  }
						  })
						  .filter(pair -> pair.second().status() == TryExecutionResult.Status.FALSIFIED)
						  .map(Pair::first)
						  .findAny();

			if (shrinkingResult.isPresent()) {
				best = shrinkingResult.get();
				return Optional.of(best);
			}

			if (invalidSamples.isEmpty()) {
				return Optional.empty();
			}

			while(!invalidSamples.isEmpty()) {
				shrinkBase = invalidSamples.removeFirst();
				if (triedFilteredSamples.contains(shrinkBase)) {
					continue;
				}
				triedFilteredSamples.add(shrinkBase);
			}

		}
	}
}
