package jqwik2.exhaustive;

import jqwik2.*;
import jqwik2.api.*;
import jqwik2.api.recording.*;

public class OrAtom extends AbstractExhaustiveSource<GenSource.Atom> implements GenSource.Atom {

	private final java.util.List<ExhaustiveAtom> alternatives;
	private int currentAlternative = 0;

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
	protected boolean tryAdvance() {
		try {
			currentAtom().next();
			return true;
		} catch (Generator.NoMoreValues e) {
			if (currentAlternative < alternatives.size() - 1) {
				currentAlternative++;
				return true;
			} else {
				reset();
				return false;
			}
		}
	}

	@Override
	public void reset() {
		currentAlternative = 0;
		alternatives.forEach(ExhaustiveAtom::reset);
	}

	@Override
	public ExhaustiveSource<Atom> clone() {
		java.util.List<ExhaustiveAtom> alternativesClones =
			alternatives.stream()
						.map(ExhaustiveAtom::clone)
						.toList();
		return new OrAtom(alternativesClones);
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
		return alternatives.get(currentAlternative);
	}

	@Override
	public Recording recording() {
		return currentAtom().recording();
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
				   ", current=" + currentAlternative +
				   '}';
	}
}
