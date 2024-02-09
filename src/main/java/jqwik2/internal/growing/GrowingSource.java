package jqwik2.internal.growing;

import java.util.*;

public interface GrowingSource<T extends GrowingSource<T>> {
	Set<T> grow();

	T copy();
}
