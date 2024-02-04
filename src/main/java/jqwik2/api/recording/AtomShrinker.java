package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

class AtomShrinker {

	private final Optional<Integer> optionalChoice;

	AtomShrinker(AtomRecording recording) {
		this.optionalChoice = recording.optionalChoice();
	}

	Stream<AtomRecording> shrink() {
		return optionalChoice.stream()
							 .flatMap(this::shrinkChoice)
							 .map(AtomRecording::new);
	}

	// TODO: Shrink in fibonacci steps from both ends
	private Stream<Integer> shrinkChoice(int choice) {
		if (choice == 0) {
			return Stream.empty();
		}
		Set<Integer> shrunkValues = new LinkedHashSet<>();
		shrunkValues.add(0);
		if (choice > 1) {
			shrunkValues.add(1);
		}
		shrunkValues.add(choice - 1);
		shrunkValues.add(choice / 2);
		return shrunkValues.stream();
	}

}
