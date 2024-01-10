package jqwik2.internal.exhaustive;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveSet extends AbstractExhaustiveSource<GenSource.List> {

	private final int size;
	private final ExhaustiveSource<?> elementSource;
	private final java.util.List<ExhaustiveSource<?>> elements = new java.util.ArrayList<>();

	public ExhaustiveSet(int size, ExhaustiveSource<?> elementSource) {
		this.size = size;
		this.elementSource = elementSource;
		createElements(size, elementSource);
	}

	private void createElements(int size, ExhaustiveSource<?> elementSource) {
		for (int i = 0; i < size; i++) {
			ExhaustiveSource<?> current = elementSource.clone();
			elements.add(current);
			if (i > 0) {
				elements.get(i - 1).chain(current);
			}
		}
	}

	public int size() {
		return size;
	}

	@Override
	public long maxCount() {
		// TODO: Consider isSet
		double maxCountDouble = Math.pow(elementSource.maxCount(), size);
		if (maxCountDouble > Long.MAX_VALUE) {
			return Exhaustive.INFINITE;
		} else {
			return (long) maxCountDouble;
		}
	}

	@Override
	protected boolean tryAdvance() {
		// TODO: Consider isSet
		return elements.getLast().advanceThisOrUp();
	}

	@Override
	public void reset() {
		elements.forEach(ExhaustiveSource::reset);
	}

	@Override
	public ExhaustiveSource<GenSource.List> clone() {
		return new ExhaustiveSet(elements.size(), elementSource);
	}

	@Override
	public boolean advance() {
		if (elements.getFirst().advance()) {
			return true;
		}
		if (prev().isEmpty()) {
			return false;
		}
		return prev().get().advanceThisOrUp();
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		elements.getLast().setSucc(exhaustive);
		super.setSucc(exhaustive);
	}

	@Override
	public String toString() {
		return "ExhaustiveSet{" +
				   "size=" + size +
				   ", elementSource=" + elementSource +
				   '}';
	}

	@Override
	public Recording recording() {
		return Recording.list(elements.stream().map(ExhaustiveSource::recording).toList());
	}
}