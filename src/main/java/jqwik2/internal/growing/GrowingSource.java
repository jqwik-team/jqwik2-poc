package jqwik2.internal.growing;

import java.util.*;

public interface GrowingSource<T extends GrowingSource<T>> {
	default Set<T> grow() {
		throw new UnsupportedOperationException("Override in each source");
	}
}
