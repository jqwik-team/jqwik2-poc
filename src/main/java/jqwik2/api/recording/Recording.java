package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public sealed interface Recording extends Comparable<Recording>
	permits ChoiceRecording, ListRecording, TupleRecording {

	Recording EMPTY = tuple(List.of());

	static Recording deserialize(String serialized) {
		return Serialization.deserialize(serialized);
	}

	Stream<? extends Recording> shrink();

	String serialize();

	default boolean isomorphicTo(Recording other) {
		return false;
	}

	static ChoiceRecording choice(int choice) {
		return new ChoiceRecording(choice);
	}

	static ChoiceRecording choice() {
		return new ChoiceRecording(Optional.empty());
	}

	static ListRecording list(Recording... elements) {
		return list(Arrays.asList(elements));
	}

	static ListRecording list(List<Recording> elements) {
		return new ListRecording(elements);
	}

	static TupleRecording tuple(Recording... elements) {
		return tuple(Arrays.asList(elements));
	}

	static TupleRecording tuple(int... choices) {
		List<Recording> choiceRecordings =
			Arrays.stream(choices).boxed()
				  .map(i -> (Recording) Recording.choice(i)).toList();
		return tuple(choiceRecordings);
	}

	static TupleRecording tuple(List<Recording> elements) {
		return new TupleRecording(elements);
	}
}
