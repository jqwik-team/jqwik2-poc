package jqwik2.api.validation;

import java.util.*;

public interface PropertyValidationResult {

	boolean isSuccessful();

	boolean isFailed();

	int countTries();

	int countChecks();

	Optional<Throwable> failure();
}
