package jqwik2.api.validation;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.description.*;
import jqwik2.internal.validation.*;

public interface PropertyValidator {

	static PropertyValidator forProperty(PropertyDescription property) {
		return new PropertyValidatorImpl(property);
	}

	default PropertyValidationResult validate() {
		return validate(PropertyValidationStrategy.DEFAULT);
	}

	PropertyValidationResult validate(PropertyValidationStrategy strategy);

	default PropertyValidationResult validateStatistically(double minPercentage, double maxStandardDeviationFactor) {
		return validateStatistically(minPercentage, maxStandardDeviationFactor, PropertyValidationStrategy.DEFAULT);
	}

	PropertyValidationResult validateStatistically(double minPercentage, double maxStandardDeviationFactor, PropertyValidationStrategy strategy);

	PropertyValidator failureDatabase(FailureDatabase database);

	PropertyValidator publisher(PlatformPublisher publisher);

	PropertyValidator publishSuccessfulResults(boolean publishSuccessfulResults);
}
