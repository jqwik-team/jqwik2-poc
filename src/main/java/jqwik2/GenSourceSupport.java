package jqwik2;

import java.util.*;

import jqwik2.recording.*;

class GenSourceSupport {
	static int chooseInt(GenSource source, int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException("min must be smaller than or equal to max");
		}
		if (min >= 0) {
			return unsignedChooseInt(source, min, max);
		}
		if (max <= 0 && min != Integer.MIN_VALUE) {
			return -unsignedChooseInt(source, Math.abs(max), Math.abs(min));
		}
		while (true) {
			GenSource.Atom intSource = source.atom();
			int maxUnsigned = min == Integer.MIN_VALUE ? Integer.MAX_VALUE :
								  Math.max(Math.abs(min), Math.abs(max)) + 1;
			int valueUnsigned = intSource.choose(maxUnsigned);
			int sign = intSource.choose(2);
			int valueWithSign = sign == 0 ? valueUnsigned : -valueUnsigned;
			if (sign == 2) { // Edge case that can never be reached by random generation
				valueWithSign = Integer.MIN_VALUE;
			}
			if (valueWithSign >= min && valueWithSign <= max) {
				return valueWithSign;
			}
		}
	}

	private static int unsignedChooseInt(GenSource source, int min, int max) {
		int range = max - min;
		int delta = source.atom().choose(range + 1);
		return min + delta;
	}

	public static Set<Recording> chooseIntEdgeCases(int min, int max) {
		if (min >= 0) {
			return smallIntEdgeCases(min, max);
		}
		if (max <= 0 && min != Integer.MIN_VALUE) {
			return smallIntEdgeCases(min, max);
		}

		Set<Recording> recordings = new LinkedHashSet<>();
		recordings.add(Recording.atom(1, 1));
		recordings.add(Recording.atom(0, 0));
		if (max >= 0) {
			if (max >= 1) {
				recordings.add(Recording.atom(1, 0));
				recordings.add(Recording.atom(max, 0));
			}
		} else {
			recordings.add(Recording.atom(Math.abs(max), 1));
		}
		if (min == Integer.MIN_VALUE) {
			recordings.add(Recording.atom(Integer.MAX_VALUE, 2));
		} else {
			recordings.add(Recording.atom(Math.abs(min), 1));
		}
		return recordings;
	}

	private static Set<Recording> smallIntEdgeCases(int min, int max) {
		int range = max - min;
		return Set.of(
			Recording.atom(0),
			Recording.atom(range)
		);
	}
}
