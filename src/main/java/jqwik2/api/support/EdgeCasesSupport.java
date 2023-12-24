package jqwik2.api.support;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.recording.*;

public class EdgeCasesSupport {

	public static Set<Recording> forAtom(Integer... maxChoicesIncluded) {
		Set<Recording> result = new LinkedHashSet<>();
		minMaxEdgeCases(Arrays.asList(maxChoicesIncluded), 0, new ArrayList<>(), result);
		return result;
	}

	private static void minMaxEdgeCases(
		List<Integer> ranges,
		int index,
		List<Integer> currentCombination,
		Set<Recording> recordings
	) {
		if (index == ranges.size()) {
			recordings.add(Recording.atom(currentCombination));
			return;
		}

		currentCombination.add(ranges.get(index));
		minMaxEdgeCases(ranges, index + 1, currentCombination, recordings);

		currentCombination.removeLast();
		currentCombination.add(0);
		minMaxEdgeCases(ranges, index + 1, currentCombination, recordings);
		currentCombination.removeLast();
	}

}
