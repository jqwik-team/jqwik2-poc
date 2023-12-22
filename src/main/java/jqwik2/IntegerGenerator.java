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
		return source.chooseInt(min, max);
	}

}
