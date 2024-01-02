package jqwik2.exhaustive;

import jqwik2.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;

public class OrAtom extends AbstractExhaustiveSource implements GenSource.Atom {

	private final java.util.List<ExhaustiveAtom> alternatives;
	private int current = 0;

	public OrAtom(ExhaustiveAtom... alternatives) {
		this(java.util.List.of(alternatives));
	}

	public OrAtom(java.util.List<ExhaustiveAtom> alternatives) {
		if (alternatives.isEmpty()) {
			throw new IllegalArgumentException("Must have at least one atom");
		}
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
		if (tryAdvance()) {
			return;
		}
		reset();
		if (prev().isPresent()) {
			prev().get().advance();
		} else {
			Generator.noMoreValues();
		}
	}

	private boolean tryAdvance() {
		try {
			currentAtom().next();
			return true;
		} catch (Generator.NoMoreValues e) {
			if (current < alternatives.size() - 1) {
				current++;
				return true;
			} else {
				reset();
				return false;
			}
		}
	}

	@Override
	public void reset() {
		current = 0;
		alternatives.forEach(ExhaustiveAtom::reset);
	}

	@Override
	public ExhaustiveSource clone() {
		return new OrAtom(alternatives);
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
		ExhaustiveAtom currentAtom = currentAtom();
		return currentAtom.choose(maxExcluded);
	}

	private ExhaustiveAtom currentAtom() {
		return alternatives.get(current);
	}

	@Override
	public Recording recording() {
		return currentAtom().recording();
	}

	@Override
	public void setPrev(Exhaustive<?> exhaustive) {
		super.setPrev(exhaustive);
		//alternatives.forEach(a -> a.setPrev(exhaustive));
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		super.setSucc(exhaustive);
		alternatives.forEach(a -> a.setSucc(exhaustive));
	}

	@Override
	public String toString() {
		return "OrAtom{" +
				   "alternatives=" + alternatives +
				   ", current=" + current +
				   '}';
	}
}
