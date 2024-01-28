package jqwik2.internal.stateful;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.stateful.*;
import jqwik2.internal.*;

class ChainInstance<S> implements Chain<S> {
	public static final int MAX_TRANSFORMER_TRIES = 1000;

	private final Generator<Transformation<S>> transformationGenerator;
	private final GenSource.List source;
	private final List<Transformer<S>> transformers = new ArrayList<>();

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
		if (isInfinite()) {
			nextTransformer = nextTransformer();
			return !nextTransformer.isEndOfChain();
		} else {
			if (transformers.size() < maxTransformations) {
				nextTransformer = nextTransformer();
				return !nextTransformer.isEndOfChain();
			} else {
				nextTransformer = null;
				return false;
			}
		}
	}

	@Override
	public S next() {
		if (!initialSupplied) {
			initialSupplied = true;
			return current;
		}
		if (nextTransformer == null) {
			throw new NoSuchElementException();
		}
		Transformer<S> transformer = nextTransformer;
		current = transformState(transformer, current);
		return current;
	}

	private Transformer<S> nextTransformer() {
		AtomicInteger attemptsCounter = new AtomicInteger(0);
		GenSource nextTransformerSource;
		try {
			nextTransformerSource = source.nextElement();
		} catch (CannotGenerateException e) {
			if (isInfinite()) {
				return Transformer.endOfChain();
			}
			this.maxTransformations = transformers.size();
			return null;
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
			transformers.add(next);
			return next;
		}

		return failWithTooManyAttempts(attemptsCounter);
	}

	private Arbitrary<Transformer<S>> nextTransformerArbitrary(AtomicInteger attemptsCounter, GenSource chooseArbitrarySource) {

		while (attemptsCounter.getAndIncrement() < MAX_TRANSFORMER_TRIES) {
			// This is important to not record failing precondition attempts (I think)
			GenSource.Atom selectTransformationSource = chooseArbitrarySource.atom();
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
}
