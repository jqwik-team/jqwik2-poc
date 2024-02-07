package jqwik2.api.statistics;

import java.util.function.*;

public interface Coverage {

	interface Checker {

		/**
		 * Check the number of occurrences returning true (ok) or false (fail)
		 *
		 * @param countChecker a predicate to accept a selected value set's number of occurrences
		 */
		void count(Predicate<Integer> countChecker);

		/**
		 * Check the percentage of occurrences returning true (ok) or false (fail)
		 *
		 * @param percentageChecker a predicate to accept a selected value set's
		 *                          percentage (0.0 - 100.0) of occurrences
		 */
		void percentage(Predicate<Double> percentageChecker);

	}

	/**
	 * Select a specific values set for coverage checking.
	 *
	 * @param values Can be anything. Must be equal to the values used in {@linkplain Statistics#collect(Object...)}
	 */
	Checker check(Object... values);
}
