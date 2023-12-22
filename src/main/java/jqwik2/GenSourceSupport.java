package jqwik2;

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
}
