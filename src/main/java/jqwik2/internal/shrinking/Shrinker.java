package jqwik2.internal.shrinking;

import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class Shrinker {
	private final Tryable tryable;
	private final Throwable originalThrowable;
	private FalsifiedSample best;

	public Shrinker(FalsifiedSample falsifiedSample, Tryable tryable) {
		this.tryable = tryable;
		best = falsifiedSample;
		originalThrowable = falsifiedSample.throwable();
	}

	public FalsifiedSample best() {
		return best;
	}

	@SuppressWarnings("OverlyLongMethod")
	public Optional<FalsifiedSample> next(int shrinkingStep) {
		Set<Sample> triedFilteredSamples = new HashSet<>();
		SortedSet<Sample> invalidSamples = new TreeSet<>();
		Sample shrinkBase = best.sample();
		while (true) {
			// System.out.println("shrinkBase: " + shrinkBase);
			// TODO: Also cache falsified and satisfied samples
			Optional<Pair<Sample, TryExecutionResult>> shrinkingResult =
				shrinkBase.shrink()
						  .map(sample -> {
							  TryExecutionResult executionResult = tryable.apply(sample);
							  return new Pair<>(sample, executionResult);
						  })
						  .filter(pair -> pair.second().status() != TryExecutionResult.Status.SATISFIED)
						  .filter(pair -> pair.first().compareTo(best.sample()) < 0)
						  .peek(pair -> {
							  TryExecutionResult result = pair.second();
							  if (isInvalid(result)) {
								  invalidSamples.add(pair.first());
							  }
						  })
						  .filter(pair -> isFalsified(pair.second()))
						  .findAny();

			if (shrinkingResult.isPresent()) {
				Sample sample = shrinkingResult.get().first();
				TryExecutionResult result = shrinkingResult.get().second();
				best = new FalsifiedSample(sample, result.throwable(), shrinkingStep);
				return Optional.of(best);
			}

			if (invalidSamples.isEmpty()) {
				return Optional.empty();
			}

			while (!invalidSamples.isEmpty()) {
				shrinkBase = invalidSamples.removeFirst();
				if (triedFilteredSamples.contains(shrinkBase)) {
					continue;
				}
				triedFilteredSamples.add(shrinkBase);
			}

		}
	}

	private boolean isFalsified(TryExecutionResult tryExecutionResult) {
		return tryExecutionResult.status() == TryExecutionResult.Status.FALSIFIED
				   && isCompatibleError(tryExecutionResult.throwable());
	}

	private boolean isInvalid(TryExecutionResult result) {
		return result.status() == TryExecutionResult.Status.INVALID ||
				   !isCompatibleError(result.throwable());
	}

	private boolean isCompatibleError(Throwable throwable) {
		if (throwable == null) {
			return originalThrowable == null;
		}
		if (originalThrowable == null) {
			return false;
		}
		// TODO: Check if stack traces point to same place. Is that worth it?
		return throwable.getClass().equals(originalThrowable.getClass());
	}
}
