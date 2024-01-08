package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class IntegerGenerator implements Generator<Integer> {
	private final int min;
	private final int max;
	private final RandomChoice.Distribution distribution;

	public IntegerGenerator(int min, int max) {
		this(min, max, RandomChoice.Distribution.UNIFORM);
	}

	public IntegerGenerator(int min, int max, RandomChoice.Distribution distribution) {
		this.min = min;
		this.max = max;
		this.distribution = distribution;
	}

	@Override
	public Integer generate(GenSource source) {
		return new IntegerGenerationSupport(source, distribution).chooseInt(min, max);
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return IntegerGenerationSupport.edgeCases(min, max);
	}

	@Override
	public Optional<? extends ExhaustiveSource<?>> exhaustive() {
		return IntegerGenerationSupport.exhaustive(min, max);
	}

	public int max() {
		return max;
	}

	public int min() {
		return min;
	}
}
