package jqwik2.internal.generators;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class CreateGenerator<T> implements Generator<T> {
	private final Supplier<T> supplier;

	public CreateGenerator(Supplier<T> supplier) {
		this.supplier = supplier;
	}

	@Override
	public T generate(GenSource source) {
		return supplier.get();
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return EdgeCasesSupport.any();
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.any();
	}
}
