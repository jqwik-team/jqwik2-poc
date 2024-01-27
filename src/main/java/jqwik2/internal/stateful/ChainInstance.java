package jqwik2.internal.stateful;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.stateful.*;

class ChainInstance<S> implements Chain<S> {
	private final Supplier<? extends S> initialSupplier;
	private final int maxTransformations;
	private final Generator<Transformation<S>> transformationGenerator;
	private final GenSource.List source;
	private final List<Transformer<S>> transformations = new ArrayList<>();

	ChainInstance(
		Supplier<? extends S> initialSupplier,
		int maxTransformations,
		Generator<Transformation<S>> transformationGenerator,
		GenSource.List source
	) {
		this.initialSupplier = initialSupplier;
		this.maxTransformations = maxTransformations;
		this.transformationGenerator = transformationGenerator;
		this.source = source;
	}

	@Override
	public Iterator<S> start() {
		return new ChainIterator(initialSupplier.get());
	}

	@Override
	public List<String> transformations() {
		return null;
	}

	@Override
	public List<Transformer<S>> transformers() {
		return null;
	}

	@Override
	public int maxTransformations() {
		return maxTransformations;
	}

	private boolean isInfinite() {
		return maxTransformations < 0;
	}

	private class ChainIterator implements Iterator<S> {
		private S current;
		private boolean initialSupplied = false;
		private int steps = 0;
		private Transformer<S> nextTransformer = null;


		public ChainIterator(S initial) {
			this.current = initial;
		}

		@Override
		public boolean hasNext() {
			if (!initialSupplied) {
				return true;
			}
			synchronized (ChainInstance.this) {
				if (isInfinite()) {
					nextTransformer = nextTransformer();
					return !nextTransformer.isEndOfChain();
				} else {
					if (steps < maxTransformations) {
						nextTransformer = nextTransformer();
						return !nextTransformer.isEndOfChain();
					} else {
						nextTransformer = null;
						return false;
					}
				}
			}
		}

		@Override
		public S next() {
			if (!initialSupplied) {
				initialSupplied = true;
				return current;
			}
			synchronized (ChainInstance.this) {
				if (nextTransformer == null) {
					throw new NoSuchElementException();
				}
				Transformer<S> transformer = nextTransformer;
				current = transformState(transformer, current);
				return current;
			}
		}

		private Transformer<S> nextTransformer() {
			return null;
		}

		private S transformState(Transformer<S> transformer, S current) {
			return null;
		}
	}
}
