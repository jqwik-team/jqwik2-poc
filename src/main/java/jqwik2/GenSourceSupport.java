package jqwik2;

class GenSourceSupport {
	static int chooseInt(GenSource source, int min, int max) {
		while (true) {
			GenSource.Atom intSource = source.atom();
			int abs = intSource.choose(Math.max(Math.abs(min), Math.abs(max)) + 1);
			int sign = intSource.choose(2);
			int valueWithSign = sign == 0 ? abs : -abs;
			if (sign == 2) { // Edge case that can never be reached by random generation
				valueWithSign = Integer.MIN_VALUE;
			}
			if (valueWithSign >= min && valueWithSign <= max) {
				return valueWithSign;
			}
		}
	}
}
