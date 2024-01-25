package jqwik2.internal.generators;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

import static jqwik2.api.recording.Recording.*;

public class GeneratorFlatMap<T, R> implements Generator<R> {
	private final Generator<T> generator;
	private final Function<T, Generator<R>> mapper;
	private final Map<T, Generator<R>> cache = new ConcurrentHashMap<>();

	public GeneratorFlatMap(Generator<T> generator, Function<T, Generator<R>> mapper) {
		this.generator = generator;
		this.mapper = mapper;
	}

	@Override
	public R generate(GenSource source) {
		var tuple = source.tuple();
		var valueToMap = generator.generate(tuple.nextValue());
		Generator<R> rGenerator = cache.computeIfAbsent(valueToMap, mapper::apply);
		return rGenerator.generate(tuple.nextValue());
	}

	@Override
	public Iterable<Recording> edgeCases() {
		// TODO: make sure this does not run infinitely in highly nested cases
		Set<Recording> recordings = new LinkedHashSet<>();
		generator.edgeCases().forEach(headRecording -> {
			Optional<T> optional = generator.fromRecording(headRecording);
			optional.ifPresent(headValue -> {
				Generator<R> childGenerator = mapper.apply(headValue);
				childGenerator.edgeCases().forEach(childRecording -> {
					recordings.add(tuple(
						headRecording,
						childRecording
					));
				});
			});
		});
		return recordings;
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.flatMap(
			generator.exhaustive(),
			head -> {
				T headValue = generator.generate(head);
				Generator<R> childGenerator = mapper.apply(headValue);
				return childGenerator.exhaustive();
			}
		);
	}
}
