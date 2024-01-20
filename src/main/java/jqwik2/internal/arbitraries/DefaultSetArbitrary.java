package jqwik2.internal.arbitraries;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.internal.generators.*;

public class DefaultSetArbitrary<T> implements SetArbitrary<T> {
	private final Arbitrary<T> elementArbitrary;
	private final int minSize;
	private final int maxSize;

	public DefaultSetArbitrary(Arbitrary<T> elementArbitrary) {
		this(elementArbitrary, 0, BaseGenerators.DEFAULT_COLLECTION_SIZE);
	}

	private DefaultSetArbitrary(Arbitrary<T> elementArbitrary, int minSize, int maxSize) {
		this.elementArbitrary = elementArbitrary;
		this.minSize = minSize;
		this.maxSize = maxSize;
	}

	@Override
	public Generator<Set<T>> generator() {
		return elementArbitrary.generator().set(minSize, maxSize);
	}

	@Override
	public SetArbitrary<T> ofMinSize(int newMinSize) {
		return new DefaultSetArbitrary<>(elementArbitrary, newMinSize, maxSize);
	}

	@Override
	public SetArbitrary<T> ofMaxSize(int newMaxSize) {
		return new DefaultSetArbitrary<>(elementArbitrary, minSize, newMaxSize);
	}
}
