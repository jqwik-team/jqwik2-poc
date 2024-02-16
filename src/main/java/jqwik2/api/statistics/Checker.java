package jqwik2.api.statistics;

import java.util.function.*;

import jqwik2.api.*;
import jqwik2.internal.statistics.*;

public interface Checker {

	static Checker check(String label, Predicate<Double> nPredicate) {
		return new StatisticsChecker().and(label, nPredicate);
	}

	Checker and(String label, Predicate<Double> nPredicate);

	GuidedGeneration guideWith(IterableSampleSource source);
}
