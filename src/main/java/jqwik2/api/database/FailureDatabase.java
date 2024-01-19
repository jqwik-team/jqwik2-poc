package jqwik2.api.database;

import java.util.*;

import jqwik2.api.recording.*;

public interface FailureDatabase {

	void saveFailure(String propertyId, SampleRecording recording);

	void deleteFailure(String propertyId, SampleRecording recording);

	void deleteProperty(String propertyId);

	Set<SampleRecording> loadFailures(String propertyId);

	void clear();

	Set<String> failingProperties();

	void saveSeed(String propertyId, String seed);

	Optional<String> loadSeed(String propertyId);
}
