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
		int[] values = source.next(1, minLength, maxLength);
		int length = values[0];
		seeds.add(length);
		for (int i = 0; i < length; i++) {
			GeneratedValue<T> element = elementGenerator.generate(source);
			value.add(element.value());
			elements.add(element);
		}
		BaseSeed base = new BaseSeed(1, minLength, maxLength, values);
		List<Seed> children = elements.stream().map(GeneratedValue::seed).toList();
		Seed seed = new ParentSeed(base, children);
		return new GeneratedValue<>(value, this, seed, seeds, elements);
	}

}
