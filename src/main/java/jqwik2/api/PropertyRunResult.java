package jqwik2.api;

import java.util.*;

import jqwik2.*;

public record PropertyRunResult(
	Status status, int countTries, int countChecks,
	SortedSet<FalsifiedSample> falsifiedSamples
) {

	public PropertyRunResult(Status status, int countTries, int countChecks) {
		this(status, countTries, countChecks, new TreeSet<>());
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

