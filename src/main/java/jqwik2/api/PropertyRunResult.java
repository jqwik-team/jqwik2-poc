package jqwik2.api;

import java.util.*;

import jqwik2.api.support.*;
import jqwik2.api.validation.*;
import org.opentest4j.*;

public record PropertyRunResult(
	PropertyValidationStatus status, int countTries, int countChecks,
	Optional<String> effectiveSeed,
	SortedSet<FalsifiedSample> falsifiedSamples,
	Optional<Throwable> failureReason, // Can be overridden e.g. by guided generation
	Optional<Throwable> abortionReason,
	boolean timedOut
) {

	public PropertyRunResult(PropertyValidationStatus status, int countTries, int countChecks, Optional<String> effectiveSeed, boolean timedOut) {
		this(status, countTries, countChecks, effectiveSeed, new TreeSet<>(), Optional.empty(), Optional.empty(), timedOut);
	}

	public boolean isSuccessful() {
		return status == PropertyValidationStatus.SUCCESSFUL;
	}

	public boolean isFailed() {
		return status == PropertyValidationStatus.FAILED;
	}

	public boolean isAborted() {
		return status == PropertyValidationStatus.ABORTED;
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

	public PropertyRunResult withStatus(PropertyValidationStatus changedStatus) {
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
	public void throwIfNotSuccessful() {
		if (isFailed()) {
			failureReason.ifPresent(ExceptionSupport::throwAsUnchecked);
			if (!falsifiedSamples.isEmpty()) {
				var falsifiedSample = falsifiedSamples.first();
				falsifiedSample.thrown().ifPresent(ExceptionSupport::throwAsUnchecked);
				var message = "Property check failed with sample {%s}".formatted(falsifiedSample.sample().values());
				var propertyCheckFailed = new AssertionFailedError(message);
				ExceptionSupport.throwAsUnchecked(propertyCheckFailed);
			}
			ExceptionSupport.throwAsUnchecked(new AssertionFailedError("Property failed for unknown reason"));
		}
		if (isAborted()) {
			abortionReason.ifPresent(ExceptionSupport::throwAsUnchecked);
			ExceptionSupport.throwAsUnchecked(new TestAbortedException("Property aborted for unknown reason"));
		}
	}
}

