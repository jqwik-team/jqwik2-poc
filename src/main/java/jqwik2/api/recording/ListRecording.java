package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record ListRecording(List<Recording> elements) implements Recording {

	@Override
	public Stream<? extends Recording> shrink() {
		return new ListShrinker(this).shrink();
	}

	@Override
	public int compareTo(Recording other) {
		if (other instanceof ListRecording otherList) {
			return RecordingsComparator.compare(elements, otherList.elements);
		}

		// TODO: In this case compare the overall size (number of choices) of the two recordings
		return 0;
	}

	@Override
	public boolean isomorphicTo(Recording other) {
		if (other instanceof ListRecording otherList) {
			if (elements.isEmpty() || otherList.elements.isEmpty()) {
				return true;
			}
			return elements.getFirst().isomorphicTo(otherList.elements.getFirst());
		}
		return false;
	}

	@Override
	public String serialize() {
		return Serialization.serialize(this);
	}

	@Override
	public String toString() {
		return serialize();
	}

}
