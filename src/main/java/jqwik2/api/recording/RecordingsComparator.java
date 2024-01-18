package jqwik2.api.recording;

import java.util.*;

class RecordingsComparator {

	static int compare(List<Recording> left, List<Recording> right) {
		List<Recording> leftSorted = new ArrayList<>(left);
		Collections.sort(leftSorted);
		List<Recording> rightSorted = new ArrayList<>(right);
		Collections.sort(rightSorted);
		int sortedComparison = compareElements(leftSorted, rightSorted);
		if (sortedComparison != 0) {
			return sortedComparison;
		}
		return compareElements(left, right);
	}

	private static int compareElements(List<Recording> left, List<Recording> right) {
		int sizeComparison = Integer.compare(left.size(), right.size());
		if (sizeComparison != 0) {
			return sizeComparison;
		}
		for (int i = 0; i < left.size(); i++) {
			int childComparison = left.get(i).compareTo(right.get(i));
			if (childComparison != 0) {
				return childComparison;
			}
		}
		return 0;
	}
}
