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
	public List<T> value(GenerationSource source) {
		List<T> value = new ArrayList<>();
		int length = source.next(1, minLength, maxLength)[0];
		for (int i = 0; i < length; i++) {
			value.add(elementGenerator.value(source));
		}
		return value;
	}
}
