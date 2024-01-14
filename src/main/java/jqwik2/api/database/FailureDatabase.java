package jqwik2.api.database;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public interface FailureDatabase {

	record PropertyFailure(
		PropertyRunResult.Status status,
		Set<List<Recording>> falsifiedSamples
	) {}

	void saveFailure(String id, PropertyFailure failure);

	void deleteFailure(String id, PropertyFailure failure);

	Optional<PropertyFailure> loadFailure(String id);

	void clear();
}
