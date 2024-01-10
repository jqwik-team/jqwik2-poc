package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveOr extends AbstractExhaustiveSource<GenSource> {

	private final java.util.List<? extends ExhaustiveSource<?>> alternatives;
	private int currentAlternative = 0;

	public ExhaustiveOr(java.util.List<? extends ExhaustiveSource<?>> alternatives) {
		if (alternatives.isEmpty()) {
			throw new IllegalArgumentException("Must have at least one alternative");
		}
		this.alternatives = alternatives;
	}

	@Override
	public long maxCount() {
		return sumUpToLongMAX(alternatives.stream().mapToLong(ExhaustiveSource::maxCount));
	}

	@Override
	protected boolean tryAdvance() {
		if (currentAlternative().advanceThisOrUp()) {
			return true;
		}
		if (currentAlternative < alternatives.size() - 1) {
			currentAlternative++;
			return true;
		} else {
			reset();
			return false;
		}
	}

	@Override
	public void reset() {
		currentAlternative = 0;
		alternatives.forEach(ExhaustiveSource::reset);
	}

	@Override
	public ExhaustiveOr clone() {
		List<? extends ExhaustiveSource<?>> alternativesClones =
			alternatives.stream()
						.map(Exhaustive::clone)
						.toList();
		return new ExhaustiveOr(alternativesClones);
	}

	private ExhaustiveSource<?> currentAlternative() {
		return alternatives.get(currentAlternative);
	}

	@Override
	public Recording recording() {
		return currentAlternative().recording();
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		super.setSucc(exhaustive);
		alternatives.forEach(a -> a.setSucc(exhaustive));
	}

	@Override
	public String toString() {
		return "ExhaustiveOr{" +
				   "alternatives=" + alternatives +
				   ", current=" + currentAlternative +
				   '}';
	}
}
