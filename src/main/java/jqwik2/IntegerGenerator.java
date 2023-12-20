package jqwik2;

public class IntegerGenerator implements Generator<Integer> {
	private final int min;
	private final int max;

	public IntegerGenerator(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public Integer generate(GenSource source) {
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
