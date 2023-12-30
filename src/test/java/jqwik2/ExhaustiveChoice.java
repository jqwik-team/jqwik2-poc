package jqwik2;

import jqwik2.api.*;

public class ExhaustiveChoice implements Exhaustive {
	private final int maxChoice;
	private int current = 0;

	private Exhaustive after = null;
	private Exhaustive before = null;

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
		if (after != null) {
			return maxChoice * after.maxCount();
		}
		return maxChoice;
	}

	@Override
	public void advance() {
		if (current < maxChoice) {
			current++;
		}
		if (current == maxChoice) {
			if (before != null) {
				reset();
				before.advance();
			} else {
				Generator.noMoreValues();
			}
		}
	}

	@Override
	public void chain(Exhaustive second) {
		this.after = second;
		second.setBefore(this);
	}

	@Override
	public void next() {
		if (after == null) {
			advance();
		} else {
			after.next();
		}
	}

	@Override
	public void setBefore(Exhaustive exhaustive) {
		this.before = exhaustive;
	}

	@Override
	public String toString() {
		return "ExhaustiveChoice(max=%d, current=%d)".formatted(maxChoice, current);
	}
}
