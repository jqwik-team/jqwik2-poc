package jqwik2gen;

public class IntegerGenerator implements Generator<Integer>{
	private final int min;
	private final int max;

	public IntegerGenerator(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public Shrinkable<Integer> generate(GenSource source) {
		while (true) {
			int abs = source.next(Math.max(Math.abs(min), Math.abs(max)) + 1);
			int sign = source.next(2);
			AtomRecording recorded = new AtomRecording(abs, sign);
			int valueWithSign = sign == 0 ? abs : -abs;
			if (valueWithSign >= min && valueWithSign <= max) {
				return new GeneratedShrinkable<>(valueWithSign, this, recorded);
			}
		}
	}
}
