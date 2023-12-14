package jqwik2gen;

public class IntegerGen implements Generator<Integer>{
	private final int min;
	private final int max;

	public IntegerGen(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public Shrinkable<Integer> generate(GenSource source) {
		return null;
	}
}
