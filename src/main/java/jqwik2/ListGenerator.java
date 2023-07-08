package jqwik2;

import java.util.*;

public class ListGenerator<T> implements ValueGenerator<List<T>> {

	private final ValueGenerator<T> elementGenerator;
	private final int minLength;
	private final int maxLength;

	public ListGenerator(ValueGenerator<T> elementGenerator, int minLength, int maxLength) {
		this.elementGenerator = elementGenerator;
		this.minLength = minLength;
		this.maxLength = maxLength;
	}

	@Override
	public GeneratedValue<List<T>> generate(GenerationSource source) {
		List<T> value = new ArrayList<>();
		List<GeneratedValue<?>> elements = new ArrayList<>();
		List<Integer> seeds = new ArrayList<>();
		int length = source.next(1, minLength, maxLength)[0];
		seeds.add(length);
		for (int i = 0; i < length; i++) {
			GeneratedValue<T> element = elementGenerator.generate(source);
			value.add(element.value());
			elements.add(element);
		}
		return new GeneratedValue<>(value, this, seeds, elements);
	}

}
