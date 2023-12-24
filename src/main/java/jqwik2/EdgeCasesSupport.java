package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.recording.*;

class EdgeCasesSupport {
	static Iterable<GenSource> chooseInt(int min, int max) {
		return GenSourceSupport.chooseIntEdgeCases(min, max)
				   .stream()
				   .map(RecordedSource::new).collect(Collectors.toSet());
	}

	static Set<Recording> forAtom(Integer... maxChoicesIncluded) {
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
