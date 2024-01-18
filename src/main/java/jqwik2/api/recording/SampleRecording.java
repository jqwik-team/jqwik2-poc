package jqwik2.api.recording;

import java.util.*;

public record SampleRecording(List<Recording> recordings) implements Comparable<SampleRecording> {

	public SampleRecording(Recording... recordings) {
		this(List.of(recordings));
	}

	public static SampleRecording deserialize(String serialized) {
		return Serialization.deserializeSample(serialized);
	}

	public String serialize() {
		return Serialization.serialize(this);
	}

	@Override
	public int compareTo(SampleRecording other) {
		return RecordingsComparator.compare(this.recordings, other.recordings);
	}

	public boolean isomorphicTo(SampleRecording other) {
		if (recordings.size() != other.recordings.size()) {
			return false;
		}
		for (int i = 0; i < recordings.size(); i++) {
			if (!recordings.get(i).isomorphicTo(other.recordings.get(i))) {
				return false;
			}
		}
		return true;
	}
}
