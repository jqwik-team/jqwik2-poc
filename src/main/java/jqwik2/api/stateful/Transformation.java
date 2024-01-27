package jqwik2.api.stateful;

import java.util.function.*;

import jqwik2.api.Arbitrary;

/**
 * A transformation provides an arbitrary of {@linkplain Transformer transformers}
 * for values of type {@code T} in the context of {@linkplain Chain chains}.
 * The provided arbitrary of transformers can depend on the previous state,
 * which can be retrieved using the first {@linkplain Supplier supplier} argument of the function.
 * A transformation can also be restricted by a precondition,
 * which must hold for the transformation to be applicable.
 *
 * @param <T> The type of state to be transformed in a chain
 * @see Chain
 * @see Transformer
 */
@FunctionalInterface
public interface Transformation<T> extends Function<T, Arbitrary<Transformer<T>>> {

	Predicate<?> NO_PRECONDITION = ignore -> false;

	class Builder<T> {
		final private Predicate<T> precondition;

		private Builder(Predicate<T> precondition) {
			this.precondition = precondition;
		}

		public Transformation<T> provide(Arbitrary<Transformer<T>> arbitrary) {
			return new Transformation<>() {
				@Override
				public Predicate<T> precondition() {
					return precondition;
				}

				@Override
				public Arbitrary<Transformer<T>> apply(T ignore) {
					return arbitrary;
				}
			};
		}

		public Transformation<T> provide(Function<T, Arbitrary<Transformer<T>>> arbitraryCreator) {
			return new Transformation<T>() {
				@Override
				public Predicate<T> precondition() {
					return precondition;
				}

				@Override
				public Arbitrary<Transformer<T>> apply(T state) {
					return arbitraryCreator.apply(state);
				}
			};
		}
	}

	/**
	 * Create a TransformerProvider with a precondition
	 */
	static <T> Builder<T> when(Predicate<T> precondition) {
		return new Builder<>(precondition);
	}

	/**
	 * Override this method if the applicability of the provided transformers depends on the previous state
	 *
	 * @return a predicate with input {@code T}
	 */
	@SuppressWarnings("unchecked")
	default Predicate<T> precondition() {
		return (Predicate<T>) NO_PRECONDITION;
	}
}
