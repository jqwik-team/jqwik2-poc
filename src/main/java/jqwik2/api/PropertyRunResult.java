package jqwik2.api;

import java.util.*;

import jqwik2.api.support.*;
import org.opentest4j.*;

public record PropertyRunResult(
	Status status, int countTries, int countChecks,
	Optional<String> effectiveSeed,
	SortedSet<FalsifiedSample> falsifiedSamples,
	Optional<Throwable> failureReason, // Can be overridden e.g. by guided generation
	Optional<Throwable> abortionReason,
	boolean timedOut
) {

	public PropertyRunResult(Status status, int countTries, int countChecks, Optional<String> effectiveSeed, boolean timedOut) {
		this(status, countTries, countChecks, effectiveSeed, new TreeSet<>(), Optional.empty(), Optional.empty(), timedOut);
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

	public Optional<Throwable> failureReason() {
		if (!isFailed()) {
			return Optional.empty();
		}
		if (failureReason.isPresent()) {
			return failureReason;
		}
		if (falsifiedSamples.isEmpty()) {
			return Optional.empty();
		}
		return falsifiedSamples.first().thrown();
	}

	public PropertyRunResult withStatus(Status changedStatus) {
		return new PropertyRunResult(
			changedStatus, countTries, countChecks, effectiveSeed,
			falsifiedSamples, failureReason, abortionReason, timedOut
		);
	}

	public PropertyRunResult withFailureReason(Throwable changedFailureReason) {
		return new PropertyRunResult(
			status, countTries, countChecks, effectiveSeed,
			falsifiedSamples, Optional.ofNullable(changedFailureReason), abortionReason, timedOut
		);
	}

	/**
	 * Throw the appropriate exception if the property run has failed.
	 */
	public void throwOnFailure() {
		if (isFailed()) {
			failureReason.ifPresent(ExceptionSupport::throwAsUnchecked);
			if (!falsifiedSamples.isEmpty()) {
				var falsifiedSample = falsifiedSamples.first();
				falsifiedSample.thrown().ifPresent(ExceptionSupport::throwAsUnchecked);
				var message = "Property check failed with sample {%s}".formatted(falsifiedSample.sample().values());
				var propertyCheckFailed = new AssertionFailedError(message);
				ExceptionSupport.throwAsUnchecked(propertyCheckFailed);
			}
			ExceptionSupport.throwAsUnchecked(new AssertionFailedError("Property failed but no failure reason available"));
		}
	}
}

