package jqwik2.api.validation;

import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.description.*;
import jqwik2.api.statistics.*;
import jqwik2.internal.validation.*;

public interface PropertyValidator {

	static PropertyValidator forProperty(PropertyDescription property) {
		return new PropertyValidatorImpl(property);
	}

	default PropertyValidationResult validate() {
		return validate(PropertyValidationStrategy.DEFAULT);
	}

	PropertyValidationResult validate(PropertyValidationStrategy strategy);

	default PropertyValidationResult validateStatistically(double minPercentage, StatisticalError allowedError) {
		return validateStatistically(minPercentage, allowedError, PropertyValidationStrategy.DEFAULT);
	}

	default PropertyValidationResult validateStatistically(double minPercentage) {
		return validateStatistically(minPercentage, JqwikDefaults.defaultAllowedStatisticalError(), PropertyValidationStrategy.DEFAULT);
	}

	PropertyValidationResult validateStatistically(double minPercentage, StatisticalError allowedError, PropertyValidationStrategy strategy);

	PropertyValidator failureDatabase(FailureDatabase database);

	PropertyValidator publisher(PlatformPublisher publisher);

	PropertyValidator publishSuccessfulResults(boolean publishSuccessfulResults);

	PropertyValidator registerTryExecutionListener(BiConsumer<TryExecutionResult, Sample> tryExecutionListener);
}
