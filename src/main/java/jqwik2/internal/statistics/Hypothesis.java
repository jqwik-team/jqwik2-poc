package jqwik2.internal.statistics;

import java.util.function.*;

class Hypothesis {

	public static final double ACCEPTANCE_BOUNDARY = 0.999;
	public static final double REJECTION_BOUNDARY = 0.001;
	private final String label;
	private final Predicate<Double> hypothesisPredicate;
	private double accept = 0.5;

	Hypothesis(String label, Predicate<Double> hypothesisPredicate) {
		this.label = label;
		this.hypothesisPredicate = hypothesisPredicate;
	}

	boolean test(double n) {
		return hypothesisPredicate.test(n);
	}

	String label() {
		return label;
	}

	void checkAndAdapt(double n) {
		synchronized (this) {
			if (test(n)) {
				accept = Math.min(1.0, accept * 0.995 + 0.005);
			} else {
				accept = Math.max(0.0, accept * 0.995 - 0.005);
			}
		}
		// System.out.printf("Hypothesis %s: %s%n", label, accept);
	}

	enum CheckResult {
		UNSTABLE, ACCEPT, REJECT
	}

	CheckResult check(double n) {
		if (test(n)) {
			return accept > ACCEPTANCE_BOUNDARY ? CheckResult.ACCEPT : CheckResult.UNSTABLE;
		} else {
			return accept < REJECTION_BOUNDARY ? CheckResult.REJECT : CheckResult.UNSTABLE;
		}
	}
}
