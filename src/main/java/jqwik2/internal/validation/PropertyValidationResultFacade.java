package jqwik2.internal.validation;

import jqwik2.api.*;
import jqwik2.api.validation.*;

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
	public int countTries() {
		return runResult.countTries();
	}

	@Override
	public int countChecks() {
		return runResult.countChecks();
	}
}
