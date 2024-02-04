package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public final class RandomGenSource implements IterableSampleSource, GenSource, GenSource.Choice, GenSource.List, GenSource.Tuple {
	private final RandomChoice random;

	// TODO: Replace this with polymorphic approach on subclasses of RandomGenSource
	private Class<? extends GenSource> currentType = GenSource.class;

	public RandomGenSource() {
		this(RandomChoice.create());
	}

	public RandomGenSource(String seed) {
		this(RandomChoice.create(seed));
	}

	public RandomGenSource(RandomChoice random) {
		this.random = random;
	}

	public Optional<String> seed() {
		return random.seed();
	}

	@Override
	public int choose(int maxExcluded) {
		return random.nextInt(maxExcluded);
	}

	@Override
	public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
		return random.nextInt(maxExcluded, distribution);
	}

	@Override
	public Choice choice() {
		return trySwitchTo(Choice.class);
	}

	@Override
	public List list() {
		return trySwitchTo(List.class);
	}

	@Override
	public Tuple tuple() {
		return trySwitchTo(Tuple.class);
	}

	private <T extends GenSource> RandomGenSource trySwitchTo(Class<T> targetType) {
		if (currentType == GenSource.class) {
			currentType = targetType;
			return this;
		} else if (currentType == targetType) {
			return this;
		} else {
			String message = "Source of type %s cannot be used as %s".formatted(
				currentType.getSimpleName(),
				targetType.getSimpleName()
			);
			throw new CannotGenerateException(message);
		}
	}

	@Override
	public GenSource nextElement() {
		return new RandomGenSource(random.split());
	}

	@Override
	public GenSource nextValue() {
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
	public Iterator<SampleSource> iterator() {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public SampleSource next() {
				return size -> {
					java.util.List<GenSource> sources = new ArrayList<>();
					for (int i = 0; i < size; i++) {
						sources.add(RandomGenSource.this.split());
					}
					return sources;
				};
			}
		};
	}

	@Override
	public boolean isThreadSafe() {
		return true;
	}
}
