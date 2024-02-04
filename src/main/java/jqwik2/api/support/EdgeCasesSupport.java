package jqwik2.api.support;

import java.util.*;

import jqwik2.api.recording.*;

public class EdgeCasesSupport {

	public static Iterable<Recording> any() {
		return Collections.singletonList(Recording.EMPTY);
	}

	public static Set<Recording> forChoice(int maxChoiceIncluded) {
		return Set.of(
			Recording.choice(0),
			Recording.choice(maxChoiceIncluded)
		);
	}

}
