package jqwik2;

import jqwik2.api.*;

public class ExhaustiveList implements GenSource.List, ExhaustiveSource {

	private final ExhaustiveSource elementSource;
	private final java.util.List<ExhaustiveSource> elements = new java.util.ArrayList<>();
	private int current = 0;

	public ExhaustiveList(int size, ExhaustiveSource elementSource) {
		this.elementSource = elementSource;
		createElements(size, elementSource);
	}

	private void createElements(int size, ExhaustiveSource elementSource) {
		for (int i = 0; i < size; i++) {
			ExhaustiveSource current = elementSource.clone();
			elements.add(current);
			if (i > 0) {
				elements.get(i - 1).chain(current);
			}
		}
	}

	public int size() {
		return elements.size();
	}

	@Override
	public long maxCount() {
		if (elements.isEmpty()) {
			return 0;
		}
		return elements.getFirst().maxCount();
	}

	@Override
	public void advance() {
		elements.getLast().advance();
	}

	@Override
	public ExhaustiveSource clone() {
		return new ExhaustiveList(elements.size(), elementSource);
	}

	@Override
	public void next() {
		current = 0;
		elements.getFirst().next();
	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {
		if (elements.isEmpty()) {
			// TODO: Handle empty list
			return;
		}
		elements.getFirst().setPrev(exhaustive);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		if (elements.isEmpty()) {
			// TODO: Handle empty list
			return;
		}
		elements.getLast().setPrev(exhaustive);
	}

	@Override
	public Atom atom() {
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public List list() {
		return this;
	}

	@Override
	public Tree tree() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public GenSource nextElement() {
		return elements.get(current++);
	}
}
