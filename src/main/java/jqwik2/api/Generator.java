package jqwik2.api;

import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;

public interface Generator<T> {

	T generate(GenSource source);

	default Iterable<Recording> edgeCases() {
		return Set.of();
	}

	/**
	 * Override if generator has inner generators that need to be decorated as well.
	 */
	default Generator<T> decorate(Function<Generator<T>, Generator<T>> decorator) {
		return decorator.apply(this);
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
		public Generator<T> decorate(Function<Generator<T>, Generator<T>> decorator) {
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
