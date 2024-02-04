package jqwik2.internal.stateful;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.stateful.*;
import jqwik2.internal.*;
import org.opentest4j.*;

class ChainInstance<S> implements Chain<S> {
	public static final int MAX_TRANSFORMER_TRIES = 1000;

	private final Generator<Transformation<S>> transformationGenerator;
	private final GenSource.List source;
	private final List<Transformer<S>> transformers = new ArrayList<>();
	private final Supplier<? extends S> initialSupplier;

	private S current;
	private int maxTransformations;
	private boolean initialSupplied = false;
	private Transformer<S> nextTransformer = null;

	ChainInstance(
		Supplier<? extends S> initialSupplier,
		int maxTransformations,
		Generator<Transformation<S>> transformationGenerator,
		GenSource.List source
	) {
		this.initialSupplier = initialSupplier;
		this.current = initialSupplier.get();
		this.maxTransformations = maxTransformations;
		this.transformationGenerator = transformationGenerator;
		this.source = source;
	}

	@Override
	public List<String> transformations() {
		return transformers.stream().map(Transformer::transformation).collect(Collectors.toList());
	}

	@Override
	public List<Transformer<S>> transformers() {
		return transformers;
	}

	@Override
	public int maxTransformations() {
		return maxTransformations;
	}

	@Override
	public Optional<S> current() {
		if (!initialSupplied) {
			return Optional.empty();
		}
		return Optional.ofNullable(current);
	}

	private boolean isInfinite() {
		return maxTransformations < 0;
	}

	@Override
	public boolean hasNext() {
		if (!initialSupplied) {
			return true;
		}
		ensureNextTransformerIsSet();
		return !nextTransformer.isEndOfChain();
	}

	private void ensureNextTransformerIsSet() {
		if (nextTransformer == null) {
			if (isInfinite()) {
				nextTransformer = nextTransformer();
			} else {
				if (transformers.size() < maxTransformations) {
					nextTransformer = nextTransformer();
				} else {
					nextTransformer = Transformer.endOfChain();
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
		ensureNextTransformerIsSet();
		if (nextTransformer.isEndOfChain()) {
			throw new NoSuchElementException();
		}
		transformers.add(nextTransformer);
		current = transformState(nextTransformer, current);
		nextTransformer = null;
		return current;
	}

	@SuppressWarnings("OverlyLongMethod")
	private Transformer<S> nextTransformer() {
		AtomicInteger attemptsCounter = new AtomicInteger(0);
		GenSource nextTransformerSource;
		try {
			nextTransformerSource = source.nextElement();
		} catch (CannotGenerateException cge) {
			if (isInfinite()) {
				return Transformer.endOfChain();
			}
			if (transformers.isEmpty()) {
				// Finite chains without transformations are not allowed
				throw new TestAbortedException();
			}
			return Transformer.endOfChain();
		}
		GenSource transformerSource = nextTransformerSource.tuple();
		GenSource chooseArbitrarySource = transformerSource.tuple().nextValue();
		GenSource chooseTransformerSource = transformerSource.tuple().nextValue();

		while (attemptsCounter.get() < MAX_TRANSFORMER_TRIES) {
			Arbitrary<Transformer<S>> transformerArbitrary = nextTransformerArbitrary(attemptsCounter, chooseArbitrarySource);
			Generator<Transformer<S>> generator = transformerArbitrary.generator();

			Transformer<S> next = generator.generate(chooseTransformerSource);
			if (next == Transformer.noop()) {
				continue;
			}
			return next;
		}

		return failWithTooManyAttempts(attemptsCounter);
	}

	private Arbitrary<Transformer<S>> nextTransformerArbitrary(AtomicInteger attemptsCounter, GenSource chooseArbitrarySource) {

		while (attemptsCounter.getAndIncrement() < MAX_TRANSFORMER_TRIES) {
			// This is important to not record failing precondition attempts (I think)
			GenSource.Choice selectTransformationSource = chooseArbitrarySource.choice();
			Transformation<S> transformation = transformationGenerator.generate(selectTransformationSource);

			if (!transformation.precondition().test(current)) {
				continue;
			}
			return transformation.apply(current);
		}
		return failWithTooManyAttempts(attemptsCounter);
	}

	private <R> R failWithTooManyAttempts(AtomicInteger attemptsCounter) {
		String message = String.format("Could not generate a transformer after %s attempts.", attemptsCounter.get());
		throw new CannotGenerateException(message);
	}

	private S transformState(Transformer<S> transformer, S current) {
		return transformer.apply(current);
	}

	@Override
	public Iterator<S> replay() {
		return new ReplayIterator();
	}

	@Override
	public String toString() {
		String finite = isInfinite() ? "infinite" : "finite";
		String status = !initialSupplied ? "not started" : hasNext() ? "in progress" : "finished";
		return "ChainInstance[%s, %s]{maxTransformations=%d, #transformations=%d, current=%s}"
				   .formatted(finite, status, maxTransformations, transformers.size(), current().map(Objects::toString)
																								.orElse("<not initialized>"));
	}

	private class ReplayIterator implements Iterator<S> {
		private final List<Transformer<S>> toReplay;
		private boolean initialSupplied = false;
		private S current;
		private int step = 0;

		private ReplayIterator() {
			toReplay = new ArrayList<>(transformers);
			current = initialSupplier.get();
		}

		@Override
		public boolean hasNext() {
			if (!initialSupplied) {
				return true;
			}
			if (step < toReplay.size()) {
				return !toReplay.get(step).isEndOfChain();
			}
			return false;
		}

		@Override
		public S next() {
			if (!initialSupplied) {
				initialSupplied = true;
				return current;
			}
			if (step >= toReplay.size()) {
				throw new NoSuchElementException();
			}
			var nextTransformer = toReplay.get(step);
			if (nextTransformer.isEndOfChain()) {
				throw new NoSuchElementException();
			}
			current = nextTransformer.apply(current);
			step++;
			return current;
		}
	}
}
