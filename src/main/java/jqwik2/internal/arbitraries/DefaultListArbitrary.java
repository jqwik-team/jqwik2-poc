package jqwik2.internal.arbitraries;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.internal.generators.*;

public class DefaultListArbitrary<T> implements ListArbitrary<T> {
	private final Arbitrary<T> elementArbitrary;
	private final int minSize;
	private final int maxSize;

	public DefaultListArbitrary(Arbitrary<T> elementArbitrary) {
		this(elementArbitrary, 0, BaseGenerators.DEFAULT_COLLECTION_SIZE);
	}

	private DefaultListArbitrary(Arbitrary<T> elementArbitrary, int minSize, int maxSize) {
		this.elementArbitrary = elementArbitrary;
		this.minSize = minSize;
		this.maxSize = maxSize;
	}

	@Override
	public Generator<List<T>> generator() {
		return elementArbitrary.generator().list(minSize, maxSize);
	}

	@Override
	public ListArbitrary<T> ofMinSize(int newMinSize) {
		return new DefaultListArbitrary<>(elementArbitrary, newMinSize, maxSize);
	}

	@Override
	public ListArbitrary<T> ofMaxSize(int newMaxSize) {
		return new DefaultListArbitrary<>(elementArbitrary, minSize, newMaxSize);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DefaultListArbitrary<?> that = (DefaultListArbitrary<?>) o;

		if (minSize != that.minSize) return false;
		if (maxSize != that.maxSize) return false;
		return elementArbitrary.equals(that.elementArbitrary);
	}

	@Override
	public int hashCode() {
		return Objects.hash(elementArbitrary, minSize, maxSize);
	}
}
