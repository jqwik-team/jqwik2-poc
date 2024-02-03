package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public sealed interface Recording extends Comparable<Recording>
		permits AtomRecording, ListRecording, TupleRecording {

	Recording EMPTY = tuple(List.of());

	static Recording deserialize(String serialized) {
		return Serialization.deserialize(serialized);
	}

	Stream<? extends Recording> shrink();

	String serialize();

	default boolean isomorphicTo(Recording other) {
		return false;
	}

	static AtomRecording atom(int choice) {
		return new AtomRecording(List.of(choice));
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

	static TupleRecording tuple(int... atoms) {
		List<Recording> atomRecordings = Arrays.stream(atoms).boxed()
											   .map(i -> (Recording) Recording.atom(i)).toList();
		return tuple(atomRecordings);
	}

	static TupleRecording tuple(List<Recording> elements) {
		return new TupleRecording(elements);
	}
}
