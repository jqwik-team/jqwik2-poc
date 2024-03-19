package jqwik2.internal.validation;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.support.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import org.opentest4j.*;

public class PropertyValidationResultFacade implements PropertyValidationResult {
	private final PropertyRunResult runResult;
	private final boolean statisticalValidation;

	public PropertyValidationResultFacade(PropertyRunResult runResult) {
		this(runResult, false);
	}

	public PropertyValidationResultFacade(PropertyRunResult runResult, boolean statisticalValidation) {
		this.runResult = runResult;
		this.statisticalValidation = statisticalValidation;
	}

	@Override
	public PropertyValidationStatus status() {
		return runResult.status();
	}

	@Override
	public boolean isSuccessful() {
		return status().isSuccessful();
	}

	@Override
	public boolean isFailed() {
		return status().isFailed();
	}

	@Override
	public boolean isAborted() {
		return status().isAborted();
	}

	@Override
	public int countTries() {
		return runResult.countTries();
	}

	@Override
	public int countChecks() {
		return runResult.countChecks();
	}

	@Override
	public Optional<Throwable> failure() {
		if (runResult.isFailed()) {
			return Optional.of(determineFailure());
		}
		if (runResult.isAborted()) {
			return Optional.of(determineAbortion());
		}
		return Optional.empty();
	}

	private Throwable determineAbortion() {
		if (runResult.abortionReason().isPresent()) {
			return runResult.abortionReason().get();
		}
		return new TestAbortedException("Property aborted for unknown reason");
	}

	private Throwable determineFailure() {
		if (runResult.failureReason().isPresent()) {
			return runResult.failureReason().get();
		}
		if (falsifiedSamples().isEmpty()) {
			return new AssertionFailedError("Property failed for unknown reason");
		}

		var smallestFalsifiedSample = falsifiedSamples().first();
		if (smallestFalsifiedSample.thrown().isPresent()) {
			return smallestFalsifiedSample.thrown().get();
		}

		var message = "Property check failed with sample {%s}".formatted(smallestFalsifiedSample.sample().values());
		return new AssertionFailedError(message);
	}

	@Override
	public SortedSet<FalsifiedSample> falsifiedSamples() {
		if (statisticalValidation) {
			return Collections.emptySortedSet();
		}
		return runResult.falsifiedSamples();
	}

	@Override
	public void throwIfNotSuccessful() {
		if (isFailed()) {
			ExceptionSupport.throwAsUnchecked(determineFailure());
		}
		if (isAborted()) {
			runResult.abortionReason().ifPresent(ExceptionSupport::throwAsUnchecked);
			ExceptionSupport.throwAsUnchecked(new TestAbortedException("Property aborted for unknown reason"));
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		PropertyValidationResultFacade that = (PropertyValidationResultFacade) o;
		return Objects.equals(runResult, that.runResult);
	}

	@Override
	public int hashCode() {
		return Objects.hash(runResult);
	}
}
