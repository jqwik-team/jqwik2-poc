package jqwik2.api.recording;

import java.util.*;
import java.util.stream.*;

public sealed interface Recording extends Comparable<Recording>
	permits AtomRecording, ListRecording, TreeRecording {

	Stream<? extends Recording> shrink();

	default boolean isomorphicTo(Recording other) {
		return false;
	}

	static AtomRecording atom(List<Integer> choices) {
		return new AtomRecording(List.copyOf(choices));
	}

	static AtomRecording atom(Integer... choices) {
		return atom(Arrays.asList(choices));
	}

	static TreeRecording tree(Recording head, Recording child) {
		return new TreeRecording(head, child);
	}

	static ListRecording list(Recording... elements) {
		return new ListRecording(Arrays.asList(elements));
	}

	static ListRecording list(List<Recording> elements) {
		return new ListRecording(elements);
	}
}
