package jqwik2.api.arbitraries;

import java.util.*;

import jqwik2.api.*;

public interface ListArbitrary<T> extends Arbitrary<List<T>> {

	default ListArbitrary<T> ofSize(int size) {
		return ofMinSize(size).ofMaxSize(size);
	}

	ListArbitrary<T> ofMinSize(int minSize);

	ListArbitrary<T> ofMaxSize(int maxSize);
}
