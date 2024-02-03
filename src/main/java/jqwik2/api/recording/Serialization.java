package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

class Serialization {

	public static final char ATOM = 'a';
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
			case ATOM:
				return deserializeAtom(serialized);
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
		return new TupleRecording(deserializeParts(elementsPart));
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
		return new AtomRecording(choices.getFirst());
	}

	private static String serializedContents(String serialized) {
		return serialized.substring(2, serialized.length() - 1);
	}

	static String serialize(SampleRecording recording) {
		return recording.recordings().stream().map(Recording::serialize).collect(Collectors.joining(":"));
	}

	static String serialize(AtomRecording recording) {
		var listOfChoices = listOfChoices(List.of(recording.choice()));
		return ATOM + "[%s]".formatted(listOfChoices);
	}

	private static String listOfChoices(List<Integer> choices) {
		return choices.stream()
					  .map(String::valueOf)
					  .collect(Collectors.joining(":"));
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
