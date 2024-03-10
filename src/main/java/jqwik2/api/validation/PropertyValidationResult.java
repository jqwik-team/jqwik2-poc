package jqwik2.api.validation;

import java.util.*;

import jqwik2.api.*;

public interface PropertyValidationResult {

	PropertyValidationStatus status();

	boolean isSuccessful();

	boolean isFailed();

	boolean isAborted();

	int countTries();

	int countChecks();

	Optional<Throwable> failure();

	SortedSet<FalsifiedSample> falsifiedSamples();

	void throwIfNotSuccessful();
}
