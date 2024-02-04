package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record AtomRecording(Optional<Integer> optionalChoice) implements Recording {

	public AtomRecording(int choice) {
		this(Optional.of(choice));
	}

	public AtomRecording {
		optionalChoice.ifPresent( choice -> {
			if (choice < 0) {
				throw new IllegalArgumentException("A choice must be >= 0");
			}
		});
	}

	@Override
	public Stream<? extends Recording> shrink() {
		return new AtomShrinker(this).shrink();
	}

	@Override
	public int compareTo(Recording other) {
		if (other instanceof AtomRecording otherAtomic) {
			return compareAtoms(this, otherAtomic);
		}
		return 0;
	}

	@Override
	public boolean isomorphicTo(Recording other) {
		if (other instanceof AtomRecording atom) {
			return this.optionalChoice.isPresent() == atom.optionalChoice.isPresent();
		}
		return false;
	}

	private int compareAtoms(AtomRecording left, AtomRecording right) {
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

	public List<Integer> choices() {
		return optionalChoice.map(List::of).orElse(List.of());
	}
}
