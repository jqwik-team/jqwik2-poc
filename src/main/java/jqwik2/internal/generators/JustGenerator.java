package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class JustGenerator<T> implements Generator<T> {
	private final T value;

	public JustGenerator(T value) {
		this.value = value;
	}

	@Override
	public T generate(GenSource source) {
		return value;
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return EdgeCasesSupport.any();
	}

	@Override
	public Optional<? extends ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.any();
	}
}
