package jqwik2.internal.recording;

import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;

public class FilteredRecordingIterator implements Iterator<Recording> {
	private final Iterator<Recording> iterator;
	private final Predicate<Recording> filter;
	private Recording next = null;

	public FilteredRecordingIterator(Iterator<Recording> iterator, Predicate<Recording> filter) {
		this.iterator = iterator;
		this.filter = filter;
		findNextElement();
	}

	@Override
	public boolean hasNext() {
		return next != null;
	}

	private void findNextElement() {
		while (iterator.hasNext()) {
			var next = iterator.next();
			if (filter.test(next)) {
				this.next = next;
				return;
			}
		}
		this.next = null;
	}

	@Override
	public Recording next() {
		if (next == null) {
			throw new NoSuchElementException();
		}
		var current = next;
		findNextElement();
		return current;
	}
}
