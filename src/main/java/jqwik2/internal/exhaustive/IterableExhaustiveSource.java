package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;

public class IterableExhaustiveSource implements IterableSampleSource {
	private final List<? extends ExhaustiveSource<?>> exhaustiveSources;
	private final long maxCount;

	public static Optional<IterableExhaustiveSource> from(Generator<?>... generators) {
		return from(Arrays.asList(generators));
	}

	public static Optional<IterableExhaustiveSource> from(List<Generator<?>> generators) {
		List<ExhaustiveSource<?>> sources = new ArrayList<>();
		for (Generator<?> generator : generators) {
			Optional<ExhaustiveSource<?>> exhaustiveSource = generator.exhaustive();
			if (exhaustiveSource.isEmpty()) {
				return Optional.empty();
			}
			sources.add(exhaustiveSource.get());
		}
		var maxCount = calculateMaxCount(sources);
		if (maxCount == Exhaustive.INFINITE) {
			return Optional.empty();
		}
		return Optional.of(new IterableExhaustiveSource(sources, maxCount));
	}

	private static long calculateMaxCount(List<? extends ExhaustiveSource<?>> sources) {
		long acc = 1;
		for (ExhaustiveSource<?> exhaustiveSource : sources) {
			long maxCount = exhaustiveSource.maxCount();
			if (maxCount == Exhaustive.INFINITE) {
				return Exhaustive.INFINITE;
			}
			if (maxCount == 0) {
				return 0;
			}
			if (Exhaustive.INFINITE / acc < maxCount) {
				return Exhaustive.INFINITE;
			}
			acc = acc * maxCount;
		}
		return acc;
	}

	private IterableExhaustiveSource(List<? extends ExhaustiveSource<?>> exhaustiveSources, long maxCount) {
		this.exhaustiveSources = exhaustiveSources;
		this.maxCount = maxCount;
	}

	public long maxCount() {
		return maxCount;
	}

	@Override
	public Iterator<SampleSource> iterator() {
		if (exhaustiveSources.isEmpty()) {
			return Collections.emptyIterator();
		}
		return new ExhaustiveGenSourceIterator(exhaustiveSources);
	}

	private static class ExhaustiveGenSourceIterator implements Iterator<SampleSource> {

		private final List<? extends ExhaustiveSource<?>> sources;

		private boolean hasNextBeenInvoked = false;

		public ExhaustiveGenSourceIterator(List<? extends ExhaustiveSource<?>> exhaustiveSources) {
			this.sources = cloneSources(exhaustiveSources);
			chainSources(sources);
		}

		private List<? extends ExhaustiveSource<?>> cloneSources(List<? extends ExhaustiveSource<?>> exhaustiveSources) {
			return exhaustiveSources.stream()
									.map(Exhaustive::clone)
									.toList();
		}

		private void chainSources(List<? extends ExhaustiveSource<?>> sources) {
			for (int i = 0; i < sources.size() - 1; i++) {
				ExhaustiveSource<?> current = sources.get(i);
				ExhaustiveSource<?> next = sources.get(i + 1);
				current.chain(next);
			}
		}

		@Override
		public boolean hasNext() {
			if (!hasNextBeenInvoked) {
				return true;
			}
			boolean advanced = sources.getFirst().advance();
			if (!advanced) {
				return false;
			}
			hasNextBeenInvoked = false;
			return true;
		}

		@Override
		public SampleSource next() {
			hasNextBeenInvoked = true;
			List<? extends GenSource> realSources = sources.stream()
														   .map(ExhaustiveSource::current)
														   .toList();
			return SampleSource.of(realSources);
		}
	}
}
