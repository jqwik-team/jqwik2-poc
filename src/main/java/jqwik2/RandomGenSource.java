package jqwik2;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public final class RandomGenSource implements IterableGenSource, GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	private final RandomChoice random;

	public RandomGenSource() {
		this(RandomChoice.create());
	}

	public RandomGenSource(String seed) {
		this(RandomChoice.create(seed));
	}

	public RandomGenSource(RandomChoice random) {
		this.random = random;
	}

	@Override
	public int choose(int maxExcluded) {
		return random.nextInt(maxExcluded);
	}

	@Override
	public Atom atom() {
		return this;
	}

	@Override
	public List list() {
		return this;
	}

	@Override
	public Tree tree() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public GenSource nextElement() {
		return new RandomGenSource(random.split());
	}

	@Override
	public GenSource head() {
		return new RandomGenSource(random.split());
	}

	@Override
	public GenSource child() {
		return new RandomGenSource(random.split());
	}

	/**
	 * Split off a new random gen source for usage in different generators.
	 *
	 * <p>This method must be thread-safe!</p>
	 *
	 * @return a new random gen source
	 */
	public synchronized RandomGenSource split() {
		return new RandomGenSource(random.split());
	}

	/**
	 * Create one element with a give probability and otherwise another element.
	 *
	 * <p>This choice will not be recorded!</p>
	 */
	public <T> T withProbability(double probability, Supplier<T> supply, Supplier<T> otherwise) {
		if (probability > 0.0 && random.nextDouble() < probability) {
			return supply.get();
		}
		return otherwise.get();
	}

	/**
	 * Choose one element randomly.
	 *
	 * <p>This choice will not be recorded!</p>
	 */
	public Recording chooseOne(Collection<Recording> edgeCases) {
		ArrayList<Recording> values = new ArrayList<>(edgeCases);
		int choice = random.nextInt(edgeCases.size());
		return values.get(choice);
	}

	@Override
	public Iterator<GenSource> iterator() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public GenSource next() {
				return RandomGenSource.this.split();
			}
		};
	}
}
