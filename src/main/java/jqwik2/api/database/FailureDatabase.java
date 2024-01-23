package jqwik2.api.database;

import java.util.*;

import jqwik2.api.recording.*;

public interface FailureDatabase {

	FailureDatabase NULL = new NullFailureDatabase();

	void saveFailingSample(String propertyId, SampleRecording recording);

	void deleteFailure(String propertyId, SampleRecording recording);

	void deleteProperty(String propertyId);

	Set<SampleRecording> loadFailingSamples(String propertyId);

	void clear();

	Set<String> failingProperties();

	void saveSeed(String propertyId, String seed);

	Optional<String> loadSeed(String propertyId);

	boolean hasFailed(String propertyId);

	/**
	 * Override for optimized implementation
	 */
	default void saveFailure(String propertyId, String seed, Set<SampleRecording> failingSamples) {
		deleteProperty(propertyId);
		saveSeed(propertyId, seed);
		for (SampleRecording sample : failingSamples) {
			saveFailingSample(propertyId, sample);
		}
	}

	class NullFailureDatabase implements FailureDatabase {
		@Override
		public void saveFailingSample(String propertyId, SampleRecording recording) {
		}

		@Override
		public void deleteFailure(String propertyId, SampleRecording recording) {
		}

		@Override
		public void deleteProperty(String propertyId) {
		}

		@Override
		public Set<SampleRecording> loadFailingSamples(String propertyId) {
			return Collections.emptySet();
		}

		@Override
		public void clear() {
		}

		@Override
		public Set<String> failingProperties() {
			return Collections.emptySet();
		}

		@Override
		public void saveSeed(String propertyId, String seed) {
		}

		@Override
		public Optional<String> loadSeed(String propertyId) {
			return Optional.empty();
		}

		@Override
		public boolean hasFailed(String propertyId) {
			return false;
		}
	}
}
