package jqwik2;

import java.util.*;
import java.util.function.*;

public class FullShrinker {

	private final FalsifiedSample falsifiedSample;
	private final Tryable tryable;

	public FullShrinker(FalsifiedSample falsifiedSample, Tryable tryable) {
		this.falsifiedSample = falsifiedSample;
		this.tryable = tryable;
	}

	public FalsifiedSample shrinkToEnd(Consumer<FalsifiedSample> eachShrinkStep) {
		Shrinker shrinker = new Shrinker(falsifiedSample, tryable);
		while (true) {
			Optional<FalsifiedSample> next = shrinker.next();
			if (next.isEmpty()) {
				return shrinker.best();
			}
			eachShrinkStep.accept(next.get());
		}
	}
}
