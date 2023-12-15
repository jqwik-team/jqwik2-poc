package jqwik2gen;

import java.util.*;

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
			int value = source.next(Math.max(Math.abs(min), Math.abs(max)) + 1);
			int sign = source.next(2);
			Integer valueWithSign = sign == 0 ? value : -value;
			if (valueWithSign >= min && valueWithSign <= max) {
				RecordedSource recorded = new AtomicSource(value, sign);
				return new GeneratedShrinkable<>(valueWithSign, this, recorded);
			}
		}
	}
}
