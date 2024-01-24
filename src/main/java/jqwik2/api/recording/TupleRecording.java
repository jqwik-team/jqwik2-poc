package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record TupleRecording(List<Recording> elements) implements Recording {

	@Override
	public Stream<? extends Recording> shrink() {
		return new TupleShrinker(this).shrink();
	}

	@Override
	public int compareTo(Recording other) {
		if (other instanceof TupleRecording otherList) {
			return RecordingsComparator.compare(elements, otherList.elements);
		}

		// TODO: In this case compare the overall size (number of choices) of the two recordings
		return 0;
	}

	@Override
	public boolean isomorphicTo(Recording other) {
		if (other instanceof TupleRecording otherTuple) {
			if (elements.size() != otherTuple.elements.size()) {
				return false;
			}
			for (int i = 0; i < elements.size(); i++) {
				if (!elements.get(i).isomorphicTo(otherTuple.elements.get(i))) {
					return false;
				}
			}
			return true;
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
