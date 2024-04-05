package jqwik2.tdd;

import java.util.*;

import jqwik2.api.recording.*;

public interface TddDatabase {

	TddDatabase NULL = new NullTddDatabase();

	void saveSample(String propertyId, String caseLabel, SampleRecording recording);

	Set<SampleRecording> loadSamples(String propertyId, String caseLabel);

	/**
	 * Override for optimized implementation
	 */
	default boolean isSamplePresent(String propertyId, String caseLabel, SampleRecording recording) {
		return loadSamples(propertyId, caseLabel).contains(recording);
	}

	void clear();

	class NullTddDatabase implements TddDatabase {
		@Override
		public void saveSample(String propertyId, String caseLabel, SampleRecording recording) {

		}

		@Override
		public Set<SampleRecording> loadSamples(String propertyId, String caseLabel) {
			return Set.of();
		}

		@Override
		public void clear() {

		}
	}
}
