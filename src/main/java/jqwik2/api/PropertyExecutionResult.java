package jqwik2.api;

import java.util.*;

import jqwik2.*;

public record PropertyExecutionResult(
	Status status, int countTries, int countChecks,
	Optional<String> seed,
	Optional<FalsifiedSample> originalSample, Optional<FalsifiedSample> shrunkSample
) {

	public PropertyExecutionResult(Status status, int countTries, int countChecks) {
		this(
			status, countTries, countChecks,
			Optional.empty(),
			Optional.empty(), Optional.empty()
		);
	}

	public enum Status {
		/**
		 * Indicates that the execution of a property was <em>successful</em>.
		 */
		SUCCESSFUL,

		/**
		 * Indicates that the execution of a property was
		 * <em>aborted</em> before the actual property method could be run.
		 */
		ABORTED,

		/**
		 * Indicates that the execution of a property has <em>failed</em>.
		 */
		FAILED
	}
}

