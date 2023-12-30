package jqwik2;

import java.util.*;

import jqwik2.api.*;

public class IterableExhaustiveSource implements IterableGenSource {
	private final List<ExhaustiveSource> exhaustiveSources;

	public IterableExhaustiveSource(ExhaustiveSource ... exhaustiveSources) {
		this.exhaustiveSources = List.of(exhaustiveSources);
		chainSources();
	}

	public long maxCount() {
		return exhaustiveSources.getFirst().maxCount();
	}

	private void chainSources() {
		for (int i = 0; i < exhaustiveSources.size() - 1; i++) {
			ExhaustiveSource current = exhaustiveSources.get(i);
			ExhaustiveSource next = exhaustiveSources.get(i + 1);
			current.chain(next);
		}
	}


	@Override
	public Iterator<MultiGenSource> iterator() {
		if (exhaustiveSources.isEmpty()) {
			return Collections.emptyIterator();
		}
		return new ExhaustiveGenSourceIterator();
	}

	// TODO: Currently only one iterator creation is supported
	private class ExhaustiveGenSourceIterator implements Iterator<MultiGenSource> {

		private final MultiGenSource source = MultiGenSource.of(exhaustiveSources);
		private boolean hasNextBeenInvoked = false;

		@Override
		public boolean hasNext() {
			if (!hasNextBeenInvoked) {
				return true;
			}
			try {
				exhaustiveSources.getFirst().next();
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
