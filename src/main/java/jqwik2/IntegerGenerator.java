package jqwik2;

import jqwik2.api.*;
import jqwik2.api.support.*;

public class IntegerGenerator implements Generator<Integer> {
	private final int min;
	private final int max;

	public IntegerGenerator(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public Integer generate(GenSource source) {
		return new IntegerGenerationSupport(source).chooseInt(min, max);
	}

	@Override
	public Iterable<GenSource> edgeCases() {
		return IntegerGenerationSupport.edgeCases(min, max);
	}
}
