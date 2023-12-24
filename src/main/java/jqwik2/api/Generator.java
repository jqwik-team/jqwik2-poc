package jqwik2.api;

import java.util.*;

public interface Generator<T> {

	T generate(GenSource source);

	default Iterable<GenSource> edgeCases() {
		return Set.of();
	}
}
