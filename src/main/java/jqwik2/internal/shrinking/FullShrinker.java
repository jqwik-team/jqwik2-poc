package jqwik2.internal.shrinking;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class FullShrinker {

	private final FalsifiedSample falsifiedSample;
	private final Tryable tryable;
	private final BiConsumer<TryExecutionResult, Sample> onTry;

	public FullShrinker(FalsifiedSample falsifiedSample, Tryable tryable) {
		this(falsifiedSample, tryable, (result, sample) -> {});
	}

	public FullShrinker(FalsifiedSample falsifiedSample, Tryable tryable, BiConsumer<TryExecutionResult, Sample> onTry) {
		this.falsifiedSample = falsifiedSample;
		this.tryable = tryable;
		this.onTry = onTry;
	}

	public FalsifiedSample shrinkToEnd(Consumer<FalsifiedSample> eachShrinkStep) {
		Shrinker shrinker = new Shrinker(falsifiedSample, tryable, onTry);
		AtomicInteger countShrinkingSteps = new AtomicInteger(0);
		while (true) {
			Optional<FalsifiedSample> next = shrinker.next(countShrinkingSteps.incrementAndGet());
			if (next.isEmpty()) {
				return shrinker.best();
			}
			eachShrinkStep.accept(next.get());
		}
	}
}
