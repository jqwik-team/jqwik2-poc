package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;

public class IterableGrowingSource implements IterableSampleSource {
	@Override
	public Iterator<SampleSource> iterator() {
		return new GrowingGenSourceIterator();
	}

	private static class GrowingGenSourceIterator implements Iterator<SampleSource> {

		private final GrowingSampleSource source = new GrowingSampleSource();

		private boolean hasAdvanceBeenInvoked = false;

		public GrowingGenSourceIterator() {
		}

		@Override
		public boolean hasNext() {
			if (!hasAdvanceBeenInvoked) {
				return true;
			}
			boolean advanced = source.advance();
			if (!advanced) {
				return false;
			}
			hasAdvanceBeenInvoked = false;
			return true;
		}

		@Override
		public SampleSource next() {
			hasAdvanceBeenInvoked = true;
			return source;
		}
	}
}
