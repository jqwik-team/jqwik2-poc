package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;

import static jqwik2.api.ExhaustiveSource.*;
import static jqwik2.api.recording.Recording.atom;

public class IntegerGenerationSupport {

	private static final RandomChoice.Distribution ONLY_0_OR_1 = (random, maxExcluded) -> random.nextInt(2);

	private final GenSource source;
	private final RandomChoice.Distribution distribution;

	public IntegerGenerationSupport(GenSource source, RandomChoice.Distribution distribution) {
		this.source = source;
		this.distribution = distribution;
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

	public static Optional<ExhaustiveSource<?>> exhaustive(int min, int max) {
		if (isPositiveUnsignedIntRange(min, max)) {
			int range = max - min;
			return ExhaustiveSource.atom(range);
		}
		if (isNegativeUnsignedIntRange(min, max)) {
			int range = max - min;
			return ExhaustiveSource.atom(range);
		}
		return or(
			ExhaustiveSource.tuple(ExhaustiveSource.atom(range(0, max)), ExhaustiveSource.atom(value(0))),
			ExhaustiveSource.tuple(ExhaustiveSource.atom(range(1, Math.abs(min))), ExhaustiveSource.atom(value(1)))
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
			GenSource.Tuple intSource = source.tuple();
			boolean isMinValueOrMaxValueRequested = min == Integer.MIN_VALUE || max == Integer.MAX_VALUE;
			int maxUnsigned = isMinValueOrMaxValueRequested
								  ? Integer.MAX_VALUE
								  : Math.max(Math.abs(min), Math.abs(max)) + 1;
			int valueUnsigned = chooseUnsignedValue(intSource.nextValue().atom(), maxUnsigned);
			int signOrMaxMin = chooseSignOrMaxMin(intSource.nextValue().atom());
			if (signOrMaxMin == 1 && valueUnsigned == 0) {
				// Optimization to generate 0 less often
				continue;
			}
			if (signOrMaxMin == 2) {
				// MAX_VALUE is generated for the case (max, 2),
				if (max == Integer.MAX_VALUE && valueUnsigned == maxUnsigned - 1) {
					return Integer.MAX_VALUE;
				} else {
					continue;
				}
			}
			if (signOrMaxMin == 3) {
				// MIN_VALUE is generated for the case (min, 3),
				if (min == Integer.MIN_VALUE && valueUnsigned == maxUnsigned - 1) {
					return Integer.MIN_VALUE;
				} else {
					continue;
				}
			}
			int valueWithSign = signOrMaxMin == 0 ? valueUnsigned : -valueUnsigned;
			if (valueWithSign >= min && valueWithSign <= max) {
				return valueWithSign;
			}
		}
	}

	private static int chooseSignOrMaxMin(GenSource.Atom intSource) {
		// 0: positive, 1: negative, 2: max, 3: min
		// The random choice is only between 0 and 1. 3 or 4 are there for the edge cases
		return intSource.choose(4, ONLY_0_OR_1);
	}

	private int chooseUnsignedValue(GenSource.Atom intSource, int maxUnsigned) {
		return intSource.choose(maxUnsigned, distribution);
	}

	private int chooseUnsignedInt(int min, int max) {
		int range = max - min;
		int delta = chooseUnsignedValue(source.atom(), range + 1);
		return min + delta;
	}

	private static Set<Recording> fullRangeIntEdgeCases(int min, int max) {
		Set<Recording> recordings = new LinkedHashSet<>();
		recordings.add(Recording.tuple(atom(Math.abs(max)), atom(0)));
		recordings.add(Recording.tuple(atom(0), atom(0)));
		recordings.add(Recording.tuple(atom(1), atom(1)));
		recordings.add(Recording.tuple(atom(1), atom(0)));

		if (min == Integer.MIN_VALUE) {
			recordings.add(Recording.tuple(atom(Integer.MAX_VALUE - 1), atom(2)));
		}
		if (max == Integer.MAX_VALUE) {
			recordings.add(Recording.tuple(atom(Integer.MAX_VALUE - 1), atom(3)));
		} else {
			recordings.add(Recording.tuple(atom(Math.abs(min)), atom(1)));
		}
		return recordings;
	}
}
