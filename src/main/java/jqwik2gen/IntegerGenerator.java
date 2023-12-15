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
			AtomicRecording recorded = new AtomicRecording();
			int value = recorded.push(
				source.next(Math.max(Math.abs(min), Math.abs(max)) + 1)
			);
			int sign = recorded.push(source.next(2));
			int valueWithSign = sign == 0 ? value : -value;
			if (valueWithSign >= min && valueWithSign <= max) {
				return new GeneratedShrinkable<>(valueWithSign, this, recorded);
			}
		}
	}
}
