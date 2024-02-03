package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record AtomRecording(int choice) implements Recording {

	public AtomRecording {
		if (choice < 0) {
			throw new IllegalArgumentException("A choice must be >= 0");
		}
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
			return true;
		}
		return false;
	}

	private int compareAtoms(AtomRecording left, AtomRecording right) {
		int sizeComparison = Integer.compare(left.choice, right.choice);
		if (sizeComparison != 0) {
			return sizeComparison;
		}
		return 0;
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
