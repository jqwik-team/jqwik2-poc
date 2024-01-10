package jqwik2.api;

import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.recording.*;

public interface Generator<T> {

	static <T> Generator<T> just(T value) {
		return create(() -> value);
	}

	static <T> Generator<T> create(Supplier<T> supplier) {
		return new CreateGenerator<>(supplier);
	}

	T generate(GenSource source);

	default Generator<List<T>> list(int minSize, int maxSize) {
		return new ListGenerator<T>(this, minSize, maxSize);
	}

	default Generator<Set<T>> set(int minSize, int maxSize) {
		return new SetGenerator<>(this, minSize, maxSize);
	}

	default <R> Generator<R> map(Function<T, R> mapper) {
		return new GeneratorMap<>(this, mapper);
	}

	default Generator<T> filter(Predicate<T> filter) {
		return new GeneratorFilter<>(this, filter, 10000);
	}

	default <R> Generator<R> flatMap(Function<T, Generator<R>> mapper) {
		return new GeneratorFlatMap<>(this, mapper);
	}

	default Iterable<Recording> edgeCases() {
		return Set.of();
	}

	default Optional<ExhaustiveSource<?>> exhaustive() {
		return Optional.empty();
	}

	default Optional<T> fromRecording(Recording recording) {
		try {
			var value = generate(new RecordedSource(recording));
			return Optional.of(value);
		} catch (CannotGenerateException ignore) {
			return Optional.empty();
		}
	}

	/**
	 * Override if generator has inner generators that need to be decorated as well.
	 */
	default Generator<T> decorate(Function<Generator<?>, Generator<?>> decorator) {
		return (Generator<T>) decorator.apply(this);
	}

	@SuppressWarnings("unchecked")
	default Generator<Object> asGeneric() {
		return (Generator<Object>) this;
	}

	class Decorator<T> implements Generator<T> {

		final protected Generator<T> generator;

		public Decorator(Generator<T> generator) {
			this.generator = generator;
		}

		@Override
		public T generate(GenSource source) {
			return generator.generate(source);
		}

		@Override
		public Iterable<Recording> edgeCases() {
			return generator.edgeCases();
		}

		@Override
		public Generator<T> decorate(Function<Generator<?>, Generator<?>> decorator) {
			return generator.decorate(decorator);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Decorator<?> other) {
				return other.generator.equals(generator);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return generator.hashCode();
		}
	}
}
