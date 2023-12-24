package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.recording.*;

public class GenSourceSupport {
	public static int chooseInt(GenSource source, int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException("min must be smaller than or equal to max");
		}
		// Max possible range is 1..MAX_VALUE
		if (isPositiveUnsignedIntRange(min, max)) {
			return chooseUnsignedInt(source, min, max);
		}
		// Max possible range is MIN_VALUE..-2
		if (isNegativeUnsignedIntRange(max)) {
			return -chooseUnsignedInt(source, Math.abs(max), Math.abs(min));
		}
		return chooseFullRangedInt(source, min, max);
	}

	private static boolean isNegativeUnsignedIntRange(int max) {
		return max < -1;
	}

	private static boolean isPositiveUnsignedIntRange(int min, int max) {
		return min > 0 || (min == 0 && max < Integer.MAX_VALUE);
	}

	private static int chooseFullRangedInt(GenSource source, int min, int max) {
		while (true) {
			GenSource.Atom intSource = source.atom();
			boolean isMinValueOrMaxValueRequested = min == Integer.MIN_VALUE || max == Integer.MAX_VALUE;
			int maxUnsigned = isMinValueOrMaxValueRequested
								  ? Integer.MAX_VALUE
								  : Math.max(Math.abs(min), Math.abs(max)) + 1;
			int valueUnsigned = intSource.choose(maxUnsigned);
			int signOrMaxMin = intSource.choose(4);
			if (signOrMaxMin == 2) {
				// MAX_VALUE is generated for the case (max, 2),
				if (max == Integer.MAX_VALUE && valueUnsigned == maxUnsigned - 1)
					return Integer.MAX_VALUE;
			}
			if (signOrMaxMin == 3) {
				// MIN_VALUE is generated for the case (min, 3),
				if (min == Integer.MIN_VALUE && valueUnsigned == maxUnsigned - 1)
					return Integer.MIN_VALUE;
			}
			int valueWithSign = (signOrMaxMin % 2) == 0 ? valueUnsigned : -valueUnsigned;
			if (valueWithSign >= min && valueWithSign <= max) {
				return valueWithSign;
			}
		}
	}

	private static int chooseUnsignedInt(GenSource source, int min, int max) {
		int range = max - min;
		int delta = source.atom().choose(range + 1);
		return min + delta;
	}

	public static Set<Recording> chooseIntEdgeCases(int min, int max) {
		if (isPositiveUnsignedIntRange(min, max)) {
			int range = max - min;
			return EdgeCasesSupport.forAtom(range);
		}
		if (isNegativeUnsignedIntRange(max)) {
			int range = max - min;
			return EdgeCasesSupport.forAtom(range);
		}
		return fullRangeIntEdgeCases(min, max);
	}

	private static Set<Recording> fullRangeIntEdgeCases(int min, int max) {
		Set<Recording> recordings = new LinkedHashSet<>();
		recordings.add(Recording.atom(Math.abs(min), 1));
		recordings.add(Recording.atom(Math.abs(max), 0));
		recordings.add(Recording.atom(0, 0));

		recordings.add(Recording.atom(1, 1));
		recordings.add(Recording.atom(1, 0));

		if (min == Integer.MIN_VALUE) {
			recordings.add(Recording.atom(Integer.MAX_VALUE - 1, 2));
		}
		if (max == Integer.MAX_VALUE) {
			recordings.add(Recording.atom(Integer.MAX_VALUE - 1, 3));
		}
		return recordings;
	}

}
