package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record SampleRecording(List<Recording> recordings) {

	public static SampleRecording deserialize(String serialized) {
		return Serialization.deserializeSample(serialized);
	}

	public String serialize() {
		return Serialization.serializeSample(recordings);
	}

}
