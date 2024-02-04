package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record ChoiceRecording(Optional<Integer> optionalChoice) implements Recording {

	public ChoiceRecording(int choice) {
		this(Optional.of(choice));
	}

	public ChoiceRecording {
		optionalChoice.ifPresent( choice -> {
			if (choice < 0) {
				throw new IllegalArgumentException("A choice must be >= 0");
			}
		});
	}

	@Override
	public Stream<? extends Recording> shrink() {
		return new ChoiceShrinker(this).shrink();
	}

	@Override
	public int compareTo(Recording other) {
		if (other instanceof ChoiceRecording otherChoice) {
			return compareChoices(this, otherChoice);
		}
		return 0;
	}

	@Override
	public boolean isomorphicTo(Recording other) {
		if (other instanceof ChoiceRecording otherChoice) {
			return this.optionalChoice.isPresent() == otherChoice.optionalChoice.isPresent();
		}
		return false;
	}

	private int compareChoices(ChoiceRecording left, ChoiceRecording right) {
		if (left.optionalChoice.isEmpty()) {
			if (right.optionalChoice.isEmpty()) {
				return 0;
			}
			return -1;
		}
		if (right.optionalChoice.isEmpty()) {
			return 1;
		}
		return Integer.compare(left.optionalChoice.get(), right.optionalChoice.get());
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
