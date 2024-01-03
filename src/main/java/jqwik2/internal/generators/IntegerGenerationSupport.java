package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;

import static jqwik2.api.ExhaustiveSource.*;

public class IntegerGenerationSupport {

	private final GenSource source;

	public IntegerGenerationSupport(GenSource source) {
		this.source = source;
	}

	/**
	 * Choose a value between min and max. Both included.
	 * min must be smaller than or equal to max.
	 *
	 * @param min A value between Integer.MIN_VALUE and Integer.MAX_VALUE
	 * @param max A value between min and Integer.MAX_VALUE
	 * @return a choice between min and max (included)
	 */
	public int chooseInt(int min, int max) {
		if (min > max) {
			throw new IllegalArgumentException("min must be smaller than or equal to max");
		}
		if (isPositiveUnsignedIntRange(min, max)) {
			return chooseUnsignedInt(min, max);
		}
		if (isNegativeUnsignedIntRange(min, max)) {
			return -chooseUnsignedInt(Math.abs(max), Math.abs(min));
		}
		return chooseFullRangedInt(min, max);
	}

	public static Collection<Recording> edgeCases(int min, int max) {
		if (isPositiveUnsignedIntRange(min, max)) {
			int range = max - min;
			return EdgeCasesSupport.forAtom(range);
		}
		if (isNegativeUnsignedIntRange(min, max)) {
			int range = max - min;
			return EdgeCasesSupport.forAtom(range);
		}
		return fullRangeIntEdgeCases(min, max);
	}

	public static ExhaustiveSource<?> exhaustive(int min, int max) {
		if (isPositiveUnsignedIntRange(min, max)) {
			int range = max - min;
			return atom(range);
		}
		if (isNegativeUnsignedIntRange(min, max)) {
			int range = max - min;
			return atom(range);
		}
		return or(
			atom(range(0, max), value(0)),
			atom(range(1, Math.abs(min)), value(1))
		);
	}

	private static boolean isNegativeUnsignedIntRange(int min, int max) {
		return max < -1 || ((max <= 0) && ((long) max - (long) min) < Integer.MAX_VALUE);
	}

	private static boolean isPositiveUnsignedIntRange(int min, int max) {
		return min > 0 || (min == 0 && max < Integer.MAX_VALUE);
	}

	private int chooseFullRangedInt(int min, int max) {
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

	private int chooseUnsignedInt(int min, int max) {
		int range = max - min;
		int delta = source.atom().choose(range + 1);
		return min + delta;
	}

	private static Set<Recording> fullRangeIntEdgeCases(int min, int max) {
		Set<Recording> recordings = new LinkedHashSet<>();
		recordings.add(Recording.atom(Math.abs(max), 0));
		recordings.add(Recording.atom(0, 0));

		recordings.add(Recording.atom(1, 1));
		recordings.add(Recording.atom(1, 0));

		if (min == Integer.MIN_VALUE) {
			recordings.add(Recording.atom(Integer.MAX_VALUE - 1, 2));
		}
		if (max == Integer.MAX_VALUE) {
			recordings.add(Recording.atom(Integer.MAX_VALUE - 1, 3));
		} else {
			recordings.add(Recording.atom(Math.abs(min), 1));
		}
		return recordings;
	}
}
