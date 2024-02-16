package jqwik2.internal.statistics;

import java.util.function.*;

class Hypothesis {
	static Hypothesis claim(String label, Predicate<Double> nPredicate) {
		return new Hypothesis(label, nPredicate);
	}

	private final String label;
	private final Predicate<Double> hypothesisPredicate;

	private Hypothesis(String label, Predicate<Double> hypothesisPredicate) {
		this.label = label;
		this.hypothesisPredicate = hypothesisPredicate;
	}

	boolean test(double n) {
		return hypothesisPredicate.test(n);
	}

	String label() {
		return label;
	}
}
