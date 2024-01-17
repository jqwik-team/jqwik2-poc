package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record SampleRecording(List<Recording> recordings) {

	public static SampleRecording deserialize(String serialized) {
		return new SampleRecording(
			Arrays.stream(serialized.split(":"))
				  .map(Recording::deserialize)
				  .toList()
		);
	}

	public String serialize() {
		return recordings.stream().map(Recording::serialize).collect(Collectors.joining(":"));
	}
}
