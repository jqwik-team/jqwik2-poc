package jqwik2.api.support;

import java.util.*;

import jqwik2.api.recording.*;

public class EdgeCasesSupport {

	public static Iterable<Recording> any() {
		return Collections.singletonList(Recording.EMPTY);
	}

	public static Set<Recording> forAtom(int maxChoiceIncluded) {
		return Set.of(
			Recording.atom(0),
			Recording.atom(maxChoiceIncluded)
		);
	}

}
