package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

import static jqwik2.api.recording.Recording.*;

class Serialization {

	public static final char CHOICE = 'a';
	public static final char LIST = 'l';
	public static final char TUPLE = 't';

	public static final char START_CONTENT = '[';
	public static final char END_CONTENT = ']';
	public static final char CONTENT_SEPARATOR = ':';

	static SampleRecording deserializeSample(String serialized) {
		return new SampleRecording(deserializeParts(serialized));
	}

	static Recording deserialize(String serialized) {
		if (serialized == null || serialized.isBlank()) {
			throw new IllegalArgumentException("Invalid serialized recording: " + serialized);
		}
		char choice = serialized.charAt(0);
		switch (choice) {
			case CHOICE:
				return deserializeChoice(serialized);
			case LIST:
				return deserializeList(serialized);
			case TUPLE:
				return deserializeTuple(serialized);
			default:
				throw new IllegalArgumentException("Unknown recording type: " + choice);
		}
	}

	private static Recording deserializeList(String serialized) {
		if (serialized.length() < 3) { //TODO: Stricter check
			throw new IllegalArgumentException("Invalid serialized list recording: " + serialized);
		}
		String elementsPart = serializedContents(serialized);
		return new ListRecording(deserializeParts(elementsPart));
	}

	private static Recording deserializeTuple(String serialized) {
		if (serialized.length() < 3) { //TODO: Stricter check
			throw new IllegalArgumentException("Invalid serialized tuple recording: " + serialized);
		}
		String elementsPart = serializedContents(serialized);
		var parts = deserializeParts(elementsPart);
		if (parts.isEmpty()) {
			return EMPTY;
		}
		return new TupleRecording(parts);
	}

	private static List<Recording> deserializeParts(String partsString) {
		List<Recording> elements = new ArrayList<>();
		int start = 0;
		int end = 0;
		while (end < partsString.length()) {
			char c = partsString.charAt(end);
			if (c == START_CONTENT) {
				end += skipToEndOfElement(partsString, end);
			} else if (c == CONTENT_SEPARATOR) {
				elements.add(deserialize(partsString.substring(start, end)));
				start = end + 1;
			}
			end++;
		}
		String lastPart = partsString.substring(start, end);
		if (!lastPart.isBlank()) {
			elements.add(deserialize(lastPart));
		}
		return elements;
	}

	private static int skipToEndOfElement(String partsString, int end) {
		char c;
		int skip = 0;
		int nesting = 1;
		while (nesting > 0) {
			skip++;
			c = partsString.charAt(end + skip);
			if (c == START_CONTENT) {
				nesting++;
			} else if (c == END_CONTENT) {
				nesting--;
			}
		}
		return skip;
	}

	static Recording deserializeChoice(String serialized) {
		if (serialized.length() < 3) {
			var message = "Invalid serialized choice recording: <%s>".formatted(serialized);
			throw new IllegalArgumentException(message);
		}
		String choicesPart = serializedContents(serialized);
		List<Integer> choices = Arrays.stream(choicesPart.split(":"))
									  .filter(s -> !s.isBlank())
									  .map(Integer::parseInt)
									  .toList();
		if (choices.size() > 1) {
			var message = "A choice cannot have more than one value but <%s> has %d".formatted(serialized, choices.size());
			throw new IllegalArgumentException(message);
		}
		if (choices.stream().anyMatch(c -> c < 0)) {
			var message = "A choice cannot be negative but <%s> is".formatted(serialized);
			throw new IllegalArgumentException(message);
		}
		if (choices.isEmpty()) {
			return new ChoiceRecording(Optional.empty());
		}
		return new ChoiceRecording(choices.getFirst());
	}

	private static String serializedContents(String serialized) {
		return serialized.substring(2, serialized.length() - 1);
	}

	static String serialize(SampleRecording recording) {
		return recording.recordings().stream().map(Recording::serialize).collect(Collectors.joining(":"));
	}

	static String serialize(ChoiceRecording recording) {
		var choiceString = recording.optionalChoice().map(String::valueOf).orElse("");
		return CHOICE + "[%s]".formatted(choiceString);
	}

	static String serialize(ListRecording recording) {
		return LIST + "[%s]".formatted(listOfElements(recording.elements()));
	}

	static String serialize(TupleRecording recording) {
		return TUPLE + "[%s]".formatted(listOfElements(recording.elements()));
	}

	private static String listOfElements(List<Recording> elements) {
		return elements.stream()
					   .map(Recording::serialize)
					   .collect(Collectors.joining(":"));
	}
}
