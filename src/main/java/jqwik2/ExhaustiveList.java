package jqwik2;

import jqwik2.api.*;

public class ExhaustiveList implements GenSource.List, ExhaustiveSource {

	private final int size;
	private final ExhaustiveSource elementSource;
	private final java.util.List<ExhaustiveSource> elements = new java.util.ArrayList<>();
	private int current = 0;

	public ExhaustiveList(int size, ExhaustiveSource elementSource) {
		this.size = size;
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
		return size;
	}

	@Override
	public long maxCount() {
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
		elements.getFirst().setPrev(exhaustive);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
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

	@Override
	public String toString() {
		return "ExhaustiveList{" +
				   "size=" + size +
				   ", elementSource=" + elementSource +
				   '}';
	}
}
