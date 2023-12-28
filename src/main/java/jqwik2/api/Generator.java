package jqwik2.api;

import java.util.*;
import java.util.function.*;

import jqwik2.api.recording.*;

public interface Generator<T> {

	T generate(GenSource source);

	default Iterable<Recording> edgeCases() {
		return Set.of();
	}

	/**
	 * Override if generator has inner generators that need to be decorated as well.
	 */
	default Generator<T> decorate(Function<Generator<T>, Generator<T>> decorator) {
		return decorator.apply(this);
	}
}
