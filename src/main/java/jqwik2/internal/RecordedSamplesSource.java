package jqwik2.internal;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.recording.*;

class RecordedSamplesSource implements IterableSampleSource {
	private final List<SampleRecording> sampleRecordings;

	RecordedSamplesSource(List<SampleRecording> sampleRecordings) {
		this.sampleRecordings = sampleRecordings;
	}

	@Override
	public Iterator<SampleSource> iterator() {
		if (sampleRecordings.isEmpty()) {
			return Collections.emptyIterator();
		}
		return new RecordedSamplesIterator();
	}

	private class RecordedSamplesIterator implements Iterator<SampleSource> {

		private final Iterator<SampleRecording> recordingIterator;

		private RecordedSamplesIterator() {
			this.recordingIterator = sampleRecordings.iterator();
		}

		@Override
		public boolean hasNext() {
			return recordingIterator.hasNext();
		}

		@Override
		public SampleSource next() {
			SampleRecording recording = recordingIterator.next();
			List<? extends GenSource> recordedSources =
				recording.recordings().stream()
						 .map(RecordedSource::of)
						 .toList();
			return SampleSource.of(recordedSources);
		}
	}
}
