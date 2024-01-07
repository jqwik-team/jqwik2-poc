package jqwik2.internal.generators;

import java.util.function.*;

import jqwik2.api.*;

public class GeneratorFlatMap<T, R>  implements Generator<R> {
	private final Generator<T> generator;
	private final Function<T, Generator<R>> mapper;

	public GeneratorFlatMap(Generator<T> generator, Function<T, Generator<R>> mapper) {
		this.generator = generator;
		this.mapper = mapper;
	}

	@Override
	public R generate(GenSource source) {
		var valueToMap = generator.generate(source);
		return mapper.apply(valueToMap).generate(source);
	}
}
