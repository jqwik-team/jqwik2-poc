package jqwik2.internal.shrinking;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class Shrinker {
	private final Tryable tryable;
	private final Throwable originalThrowable;
	private final BiConsumer<TryExecutionResult, Sample> onTry;
	private FalsifiedSample best;

	public Shrinker(FalsifiedSample falsifiedSample, Tryable tryable) {
		this(falsifiedSample, tryable, (result, sample) -> {});
	}

	public Shrinker(FalsifiedSample falsifiedSample, Tryable tryable, BiConsumer<TryExecutionResult, Sample> onTry) {
		this.tryable = tryable;
		this.best = falsifiedSample;
		this.originalThrowable = falsifiedSample.throwable();
		this.onTry = onTry;
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
			Optional<Pair<Sample, TryExecutionResult>> shrinkingResult =
				shrinkBase.shrink()
						  .map(sample -> executeTry(sample))
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

	private Pair<Sample, TryExecutionResult> executeTry(Sample sample) {
		// TODO: Cache all results and take from cache if available
		TryExecutionResult executionResult = tryable.apply(sample);
		onTry.accept(executionResult, sample);
		return new Pair<>(sample, executionResult);
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
