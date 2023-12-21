package jqwik2.recording;

import java.util.*;
import java.util.stream.*;

public record AtomRecording(List<Integer> choices) implements ChoicesRecording {
	public AtomRecording(Integer... choices) {
		this(new ArrayList<>(Arrays.asList(choices)));
	}

	@Override
	public Stream<? extends ChoicesRecording> shrink() {
		return new AtomShrinker(this).shrink();
	}

	@Override
	public int compareTo(ChoicesRecording other) {
		if (other instanceof AtomRecording otherAtomic) {
			return compareAtoms(this, otherAtomic);
		}
		return 0;
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
	public String toString() {
		List<String> listOfStrings = choices.stream().map(Object::toString).toList();
		return "atom{%s}".formatted(String.join(", ", listOfStrings));
	}
}
