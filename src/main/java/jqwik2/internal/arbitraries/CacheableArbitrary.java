package jqwik2.internal.arbitraries;

import java.util.*;

import jqwik2.api.*;

public abstract class CacheableArbitrary<T> implements Arbitrary<T> {

	private final Object[] equalityParts;

	public CacheableArbitrary(Object...equalityParts) {
		this.equalityParts = equalityParts;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}
		CacheableArbitrary<?> other = (CacheableArbitrary<?>) obj;
		return Arrays.equals(this.equalityParts, other.equalityParts);
	}

	@Override
	public int hashCode() {
		return Objects.hash(equalityParts);
	}
}
