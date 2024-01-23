package jqwik2.api;

import java.util.*;

public record PropertyRunResult(
	Status status, int countTries, int countChecks,
	Optional<String> effectiveSeed,
	SortedSet<FalsifiedSample> falsifiedSamples,
	Optional<Throwable> abortionReason,
	boolean timedOut
) {

	public PropertyRunResult(Status status, int countTries, int countChecks, Optional<String> effectiveSeed, boolean timedOut) {
		this(status, countTries, countChecks, effectiveSeed, new TreeSet<>(), Optional.empty(), timedOut);
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

	public boolean isSuccessful() {
		return status == Status.SUCCESSFUL;
	}

	public boolean isFailed() {
		return status == Status.FAILED;
	}

	public boolean isAborted() {
		return status == Status.ABORTED;
	}
}

