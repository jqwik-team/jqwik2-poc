package jqwik2;

import jqwik2.api.*;

public class ExhaustiveChoice extends AbstractExhaustive implements Exhaustive {
	private final int maxChoice;
	private int current = 0;

	public ExhaustiveChoice(int maxExcluded) {
		this.maxChoice = maxExcluded;
	}

	public int choose(int maxExcluded) {
		return current % maxExcluded;
	}

	public void reset() {
		current = 0;
	}

	@Override
	public long maxCount() {
		if (succ != null) {
			return maxChoice * succ.maxCount();
		}
		return maxChoice;
	}

	@Override
	public void advance() {
		if (current < maxChoice) {
			current++;
		}
		if (current == maxChoice) {
			if (prev != null) {
				reset();
				prev.advance();
			} else {
				Generator.noMoreValues();
			}
		}
	}

	@Override
	public String toString() {
		return "ExhaustiveChoice(max=%d, current=%d)".formatted(maxChoice, current);
	}
}
