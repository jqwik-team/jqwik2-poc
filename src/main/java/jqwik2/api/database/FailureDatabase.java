package jqwik2.api.database;

import java.util.*;

import jqwik2.api.recording.*;

public interface FailureDatabase {

	void saveFailingSample(String propertyId, SampleRecording recording);

	void deleteFailure(String propertyId, SampleRecording recording);

	void deleteProperty(String propertyId);

	Set<SampleRecording> loadFailingSamples(String propertyId);

	void clear();

	Set<String> failingProperties();

	void saveSeed(String propertyId, String seed);

	Optional<String> loadSeed(String propertyId);

	/**
	 * Override for optimized implementation
	 */
	default void saveFailure(String propertyId, String seed, Set<SampleRecording> failingSamples) {
		saveSeed(propertyId, seed);
		for (SampleRecording sample : failingSamples) {
			saveFailingSample(propertyId, sample);
		}
	}
}
