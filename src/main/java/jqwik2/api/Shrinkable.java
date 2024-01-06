package jqwik2.api;

import java.util.stream.*;

import jqwik2.api.recording.*;
import jqwik2.internal.recording.*;

public interface Shrinkable<T> extends Comparable<Shrinkable<T>> {
	T value();

	Generator<T> generator();

	Recording recording();

	Stream<Shrinkable<T>> shrink();

	default T regenerate() {
		return generator().fromRecording(recording()).orElseThrow();
	}

	@SuppressWarnings("unchecked")
	default Shrinkable<Object> asGeneric() {
		return (Shrinkable<Object>) this;
	}
}

