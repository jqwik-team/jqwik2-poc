package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveSet extends ExhaustiveList {

	private Set<Set<Recording>> traversedSets = new HashSet<>();

	public ExhaustiveSet(int size, ExhaustiveSource<?> elementSource) {
		super(size, elementSource);
		if (!isCurrentANewSet()) {
			advanceToNextSet();
		}
	}

	private boolean advanceToNextSet() {
		if (size < 2) {
			return elements.getFirst().advance();
		}
		while (true) {
			boolean advanced = elements.getFirst().advance();
			if (!advanced) {
				break;
			}
			if (isCurrentANewSet()) {
				return true;
			}
		}
		return false;
	}

	private boolean isCurrentANewSet() {
		ListRecording current = (ListRecording) recording();
		Set<Recording> setRecording = new HashSet<>(current.elements());
		if (!traversedSets.contains(setRecording)
				&& current.elements().size() == setRecording.size()
		) {
			traversedSets.add(setRecording);
			return true;
		}
		return false;
	}

	public int size() {
		return size;
	}

	@Override
	public long maxCount() {
		long maxCountElement = elementSource.maxCount();
		if (maxCountElement < size) {
			return 0;
		}
		return binomialCoefficient(maxCountElement, size);
	}

	private long binomialCoefficient(long maxCountElement, int size) {
		long result = 1;
		for (int i = 0; i < size; i++) {
			// To prevent overflow
			if (Long.MAX_VALUE / (maxCountElement - i) < result) {
				return Long.MAX_VALUE;
			}
			result *= maxCountElement - i;
			result /= i + 1;
		}
		return result;
	}

	@Override
	protected boolean tryAdvance() {
		return advanceToNextSet();
	}

	@Override
	public ExhaustiveSource<GenSource.List> clone() {
		return new ExhaustiveSet(elements.size(), elementSource);
	}

	@Override
	public boolean advance() {
		if (advanceToNextSet()) {
			return true;
		}
		if (prev().isEmpty()) {
			return false;
		}
		return prev().get().advanceThisOrUp();
	}

	@Override
	public String toString() {
		return "ExhaustiveSet{" +
				   "size=" + size +
				   ", elementSource=" + elementSource +
				   '}';
	}
}
