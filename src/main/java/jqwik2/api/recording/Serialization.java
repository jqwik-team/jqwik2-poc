package jqwik2.api.recording;

import java.util.*;

class Serialization {

	static Recording deserialize(String serialized) {
		if (serialized == null || serialized.isBlank()) {
			throw new IllegalArgumentException("Invalid recording serialization: " + serialized);
		}
		char choice = serialized.charAt(0);
		switch (choice) {
			case 'a':
				return deserializeAtom(serialized);
			default:
				throw new IllegalArgumentException("Unknown recording type: " + choice);
		}
	}
	static Recording deserializeAtom(String serialized) {
		if (serialized.length() < 3) {
			throw new IllegalArgumentException("Invalid serialized atom recording: " + serialized);
		}
		String choicesPart = serializedContents(serialized);
		List<Integer> choices = Arrays.stream(choicesPart.split(":"))
									  .filter(s -> !s.isBlank())
									  .map(Integer::parseInt)
									  .toList();
		if (choices.isEmpty()) return Recording.EMPTY;

		return new AtomRecording(choices);
	}

	private static String serializedContents(String serialized) {
		return serialized.substring(2, serialized.length() - 1);
	}

	public static String serializeAtom(List<Integer> choices) {
		var listOfChoices = String.join(":",choices.stream()
										   .map(String::valueOf)
										   .toList());
		return "a[%s]".formatted(listOfChoices);
	}

}
