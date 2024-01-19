package jqwik2.api.arbitraries;

import java.util.*;

import jqwik2.api.*;

public interface SetArbitrary<T> extends Arbitrary<Set<T>> {

	default SetArbitrary<T> ofSize(int size) {
		return ofMinSize(size).ofMaxSize(size);
	}

	SetArbitrary<T> ofMinSize(int minSize);

	SetArbitrary<T> ofMaxSize(int maxSize);
}
