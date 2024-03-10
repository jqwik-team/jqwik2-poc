package jqwik2.internal.validation;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.support.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import org.opentest4j.*;

public class PropertyValidationResultFacade implements PropertyValidationResult {
	private final PropertyRunResult runResult;

	public PropertyValidationResultFacade(PropertyRunResult runResult) {
		this.runResult = runResult;
	}

	@Override
	public boolean isSuccessful() {
		return runResult.isSuccessful();
	}

	@Override
	public boolean isFailed() {
		return runResult.isFailed();
	}

	@Override
	public boolean isAborted() {
		return runResult.isAborted();
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
		if (!runResult.isFailed()) {
			return Optional.empty();
		}
		return Optional.of(determineFailure());
	}

	private Throwable determineFailure() {
		if (runResult.failureReason().isPresent()) {
			return runResult.failureReason().get();
		}
		if (runResult.falsifiedSamples().isEmpty()) {
			return new AssertionFailedError("Property failed for unknown reason");
		}

		var smallestFalsifiedSample = runResult.falsifiedSamples().first();
		if (smallestFalsifiedSample.thrown().isPresent()) {
			return smallestFalsifiedSample.thrown().get();
		}

		var message = "Property check failed with sample {%s}".formatted(smallestFalsifiedSample.sample().values());
		return new AssertionFailedError(message);
	}

	@Override
	public SortedSet<FalsifiedSample> falsifiedSamples() {
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
}
