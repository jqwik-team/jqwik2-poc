package jqwik2;

import jqwik2.api.*;

public class OrAtom implements GenSource.Atom, ExhaustiveSource {

	private final java.util.List<ExhaustiveAtom> alternatives;
	private int current = 0;

	public OrAtom(ExhaustiveAtom... alternatives) {
		this(java.util.List.of(alternatives));
	}

	public OrAtom(java.util.List<ExhaustiveAtom> alternatives) {
		if (alternatives.stream().map(ExhaustiveAtom::cardinality).distinct().count() != 1) {
			throw new IllegalArgumentException("All atoms must have the same cardinality");
		}
		this.alternatives = alternatives;
	}

	@Override
	public long maxCount() {
		return alternatives.stream().mapToLong(ExhaustiveAtom::maxCount).sum();
	}

	@Override
	public void advance() {
	}

	@Override
	public ExhaustiveSource clone() {
		return new OrAtom(alternatives);
	}

	@Override
	public void next() {
		ExhaustiveAtom currentAtom = alternatives.get(current);
		try {
			currentAtom.next();
		} catch (Generator.NoMoreValues e) {
			current++;
			if (current >= alternatives.size()) {
				throw new Generator.NoMoreValues();
			}
		}
	}

	@Override
	public void setPrev(Exhaustive exhaustive) {
	}

	@Override
	public void setSucc(Exhaustive exhaustive) {
	}

	@Override
	public Atom atom() {
		return this;
	}

	@Override
	public List list() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public Tree tree() {
		throw new CannotGenerateException("Source is not a tree");
	}

	@Override
	public int choose(int maxExcluded) {
		ExhaustiveAtom currentAtom = alternatives.get(current);
		return currentAtom.choose(maxExcluded);
	}

}
