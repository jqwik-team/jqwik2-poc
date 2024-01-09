package jqwik2.internal.generators;

import jqwik2.api.*;

public class JustGenerator<T> implements Generator<T> {
	private final T value;

	public JustGenerator(T value) {
		this.value = value;
	}

	@Override
	public T generate(GenSource source) {
		return value;
	}
}
