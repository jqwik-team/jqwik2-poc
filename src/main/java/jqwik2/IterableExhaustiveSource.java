package jqwik2;

import java.util.*;

import jqwik2.api.*;

public class IterableExhaustiveSource implements IterableGenSource {
	private final List<ExhaustiveSource> exhaustiveSources;

	public IterableExhaustiveSource(ExhaustiveSource... exhaustiveSources) {
		this.exhaustiveSources = List.of(exhaustiveSources);
	}

	public IterableExhaustiveSource(List<ExhaustiveSource> exhaustiveSources) {
		this.exhaustiveSources = exhaustiveSources;
	}

	public static IterableExhaustiveSource from(Generator<?>... generators) {
		List<ExhaustiveSource> list =
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

		private final MultiGenSource source;
		private final ExhaustiveSource first;

		private boolean hasNextBeenInvoked = false;

		public ExhaustiveGenSourceIterator(List<ExhaustiveSource> exhaustiveSources) {
			List<ExhaustiveSource> sources = cloneSources(exhaustiveSources);
			chainSources(sources);
			this.source = MultiGenSource.of(sources);
			this.first = sources.getFirst();
		}

		private List<ExhaustiveSource> cloneSources(List<ExhaustiveSource> exhaustiveSources) {
			return exhaustiveSources.stream()
									.map(exhaustiveSource -> exhaustiveSource.clone())
									.toList();
		}

		private void chainSources(List<ExhaustiveSource> sources) {
			for (int i = 0; i < sources.size() - 1; i++) {
				ExhaustiveSource current = sources.get(i);
				ExhaustiveSource next = sources.get(i + 1);
				current.chain(next);
			}
		}

		@Override
		public boolean hasNext() {
			if (!hasNextBeenInvoked) {
				return true;
			}
			try {
				first.next();
				hasNextBeenInvoked = false;
				return true;
			} catch (Generator.NoMoreValues e) {
				return false;
			}
		}

		@Override
		public MultiGenSource next() {
			hasNextBeenInvoked = true;
			return source;
		}
	}
}
