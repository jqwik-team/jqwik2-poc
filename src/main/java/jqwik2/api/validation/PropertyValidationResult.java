package jqwik2.api.validation;

import java.util.*;

import jqwik2.api.*;

public interface PropertyValidationResult {

	boolean isSuccessful();

	boolean isFailed();

	int countTries();

	int countChecks();

	Optional<Throwable> failure();

	SortedSet<FalsifiedSample> falsifiedSamples();
}
