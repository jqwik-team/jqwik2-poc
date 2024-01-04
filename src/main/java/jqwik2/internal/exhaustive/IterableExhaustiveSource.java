package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;

public class IterableExhaustiveSource implements IterableGenSource {
	private final List<? extends ExhaustiveSource<?>> exhaustiveSources;

	public static IterableExhaustiveSource from(Generator<?>... generators) {
		List<? extends ExhaustiveSource<?>> list =
			Arrays.stream(generators)
				  .map(Generator::exhaustive)
				  .map(exhaustiveSource -> exhaustiveSource.orElseThrow(
					  () -> {
						  String message = "All generators must be exhaustive";
						  return new IllegalArgumentException(message);
					  }
				  ))
				  .toList();
		return new IterableExhaustiveSource(list);
	}

	private IterableExhaustiveSource(List<? extends ExhaustiveSource<?>> exhaustiveSources) {
		this.exhaustiveSources = exhaustiveSources;
	}

	public long maxCount() {
		return exhaustiveSources.stream()
								.mapToLong(ExhaustiveSource::maxCount)
								.reduce(1, (a, b) -> a * b);
	}

	@Override
	public Iterator<MultiGenSource> iterator() {
		if (exhaustiveSources.isEmpty()) {
			return Collections.emptyIterator();
		}
		return new ExhaustiveGenSourceIterator(exhaustiveSources);
	}

	private static class ExhaustiveGenSourceIterator implements Iterator<MultiGenSource> {

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
		public MultiGenSource next() {
			hasNextBeenInvoked = true;
			List<? extends GenSource> realSources = sources.stream()
														   .map(ExhaustiveSource::current)
														   .toList();
			return MultiGenSource.of(realSources);
		}
	}
}
