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

	private class ChainIterator implements Iterator<S> {
		private S current;
		private boolean initialSupplied = false;

		public ChainIterator(S initial) {
			this.current = initial;
		}

		@Override
		public boolean hasNext() {
			if (!initialSupplied) {
				return true;
			}
			return false;
		}

		@Override
		public S next() {
			if (!initialSupplied) {
				initialSupplied = true;
				return current;
			}
			throw new NoSuchElementException();
		}
	}
}
