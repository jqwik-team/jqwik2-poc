package jqwik2.exhaustive;

import jqwik2.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveList
	extends AbstractExhaustiveSource
	implements GenSource.List {

	private final int size;
	private final ExhaustiveSource elementSource;
	private final java.util.List<ExhaustiveSource> elements = new java.util.ArrayList<>();
	private int currentElement = 0;

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
	protected boolean tryAdvance() {
		try {
			elements.getLast().advance();
			return true;
		} catch (Generator.NoMoreValues e) {
			return false;
		}
	}

	@Override
	public void reset() {
		currentElement = 0;
		elements.forEach(ExhaustiveSource::reset);
	}

	@Override
	public ExhaustiveSource clone() {
		return new ExhaustiveList(elements.size(), elementSource);
	}

	@Override
	public void next() {
		currentElement = 0;
		elements.getFirst().next();
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		super.setSucc(exhaustive);
		elements.getLast().setSucc(exhaustive);
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
		return elements.get(currentElement++);
	}

	@Override
	public String toString() {
		return "ExhaustiveList{" +
				   "size=" + size +
				   ", elementSource=" + elementSource +
				   '}';
	}

	@Override
	public Recording recording() {
		return Recording.list(elements.stream().map(ExhaustiveSource::recording).toList());
	}
}
