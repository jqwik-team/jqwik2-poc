package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public record AtomRecording(List<Integer> choices) implements Recording {

	public AtomRecording {
		if (choices.stream().anyMatch(c -> c < 0)) {
			throw new IllegalArgumentException("Atom choices must be >= 0");
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
			return choices.size() == atom.choices.size();
		}
		return false;
	}

	private int compareAtoms(AtomRecording left, AtomRecording right) {
		int sizeComparison = Integer.compare(left.choices().size(), right.choices().size());
		if (sizeComparison != 0) {
			return sizeComparison;
		}
		for (int i = 0; i < left.choices().size(); i++) {
			int seedComparison = Integer.compare(left.choices().get(i), right.choices().get(i));
			if (seedComparison != 0) {
				return seedComparison;
			}
		}
		return 0;
	}

	@Override
	public String serialize() {
		return "a[%s]".formatted(listOfChoices());
	}

	@Override
	public String toString() {
		return "atom{%s}".formatted(listOfChoices());
	}

	private String listOfChoices() {
		List<String> listOfStrings = choices.stream().map(Object::toString).toList();
		String listOfChoices = String.join(",", listOfStrings);
		return listOfChoices;
	}
}
