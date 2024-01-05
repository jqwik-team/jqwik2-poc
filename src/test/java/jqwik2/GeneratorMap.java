package jqwik2;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class GeneratorMap<T, R> implements Generator<R> {

	private final Generator<T> generator;
	private final Function<T, R> mapper;

	public GeneratorMap(Generator<T> generator, Function<T, R> mapper) {
		this.generator = generator;
		this.mapper = mapper;
	}

	@Override
	public R generate(GenSource source) {
		return mapper.apply(generator.generate(source));
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return generator.edgeCases();
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return generator.exhaustive();
	}

	@Override
	public Generator<R> decorate(Function<Generator<?>, Generator<?>> decorator) {
		return new GeneratorMap<>(generator.decorate(decorator), mapper);
	}
}
