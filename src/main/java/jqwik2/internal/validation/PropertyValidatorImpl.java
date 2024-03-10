package jqwik2.internal.validation;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.validation.*;

public class PropertyValidatorImpl implements PropertyValidator {

	private final PropertyDescription property;

	public PropertyValidatorImpl(PropertyDescription property) {
		this.property = property;
	}

	public PropertyValidationResult validate() {
		var result = new PropertyRunResult(
			PropertyRunResult.Status.SUCCESSFUL,
			100,
			100,
			Optional.empty(),
			false
		);
		return new PropertyValidationResult() {
			@Override
			public boolean isSuccessful() {
				return result.isSuccessful();
			}

			@Override
			public int countTries() {
				return result.countTries();
			}

			@Override
			public int countChecks() {
				return result.countChecks();
			}
		};
	}
}
