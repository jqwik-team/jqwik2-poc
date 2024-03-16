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

	PropertyValidator failureDatabase(FailureDatabase database);

	PropertyValidator publisher(Publisher publisher);

	PropertyValidator publishSuccessfulResults(boolean publishSuccessfulResults);

	PropertyValidationResult validate(PropertyValidationStrategy strategy);
}
