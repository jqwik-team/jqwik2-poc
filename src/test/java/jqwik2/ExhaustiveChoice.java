package jqwik2;

import jqwik2.api.*;

public class ExhaustiveChoice implements Exhaustive {
	private final int maxChoice;
	private int current = 0;

	private Exhaustive succ = null;
	private Exhaustive prev = null;

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
	public void chain(Exhaustive second) {
		this.succ = second;
		second.setPrev(this);
	}

	@Override
	public void next() {
		if (succ == null) {
			advance();
		} else {
			succ.next();
		}
	}

	@Override
	public void setPrev(Exhaustive exhaustive) {
		this.prev = exhaustive;
	}

	@Override
	public void setSucc(Exhaustive exhaustive) {
		this.succ = exhaustive;
	}

	@Override
	public String toString() {
		return "ExhaustiveChoice(max=%d, current=%d)".formatted(maxChoice, current);
	}
}
