package jqwik2.tdd;

import java.util.*;

import jqwik2.api.recording.*;

public interface TddDatabase {

	TddDatabase NULL = new NullTddDatabase();

	void saveSample(String caseId, SampleRecording recording);

	void deleteCase(String caseId);

	Set<SampleRecording> loadSamples(String caseId);

	void clear();

	class NullTddDatabase implements TddDatabase {
		@Override
		public void saveSample(String caseId, SampleRecording recording) {

		}

		@Override
		public void deleteCase(String caseId) {

		}

		@Override
		public Set<SampleRecording> loadSamples(String caseId) {
			return Set.of();
		}

		@Override
		public void clear() {

		}
	}
}
