package jqwik2.api;

import java.util.*;

import jqwik2.api.recording.*;

public interface Generator<T> {

	T generate(GenSource source);

	default Iterable<Recording> edgeCases() {
		return Set.of();
	}
}
