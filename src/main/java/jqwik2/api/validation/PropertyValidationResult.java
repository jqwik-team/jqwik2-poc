package jqwik2.api.validation;

public interface PropertyValidationResult {
	boolean isSuccessful();

	int countTries();

	int countChecks();
}
