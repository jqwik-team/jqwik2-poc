package jqwik2.internal.generators;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

class LazyGenerator<T> implements Generator<T> {
	private final Supplier<Generator<T>> generatorSupplier;
	private Generator<T> generator;

	public LazyGenerator(Supplier<Generator<T>> generatorSupplier) {
		this.generatorSupplier = generatorSupplier;
	}

	@Override
	public T generate(GenSource source) {
		return generator().generate(source);
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return generator().exhaustive();
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return generator().edgeCases();
	}

	private Generator<T> generator() {
		if (generator == null) {
			generator = generatorSupplier.get();
		}
		return generator;
	}
}
