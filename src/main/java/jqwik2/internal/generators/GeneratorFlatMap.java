package jqwik2.internal.generators;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

import static jqwik2.api.recording.Recording.*;

public class GeneratorFlatMap<T, R> implements Generator<R> {
	private final Generator<T> generator;
	private final Function<T, Generator<R>> mapper;

	public GeneratorFlatMap(Generator<T> generator, Function<T, Generator<R>> mapper) {
		this.generator = generator;
		this.mapper = mapper;
	}

	@Override
	public R generate(GenSource source) {
		var tree = source.tree();
		var valueToMap = generator.generate(tree.head());
		// TODO: The dependent generator could be cached by its source recording
		return mapper.apply(valueToMap).generate(tree.child());
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
					recordings.add(tree(
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
		return ExhaustiveSource.tree(
				generator.exhaustive(),
			head -> {
				T headValue = generator.generate(head);
				Generator<R> childGenerator = mapper.apply(headValue);
				return childGenerator.exhaustive();
			}
		);
	}
}
