package jqwik2;

import java.util.*;

import jqwik2.api.*;

public record FalsifiedSample(Sample sample, Throwable throwable) implements Comparable<FalsifiedSample> {
	Optional<Throwable> thrown() {
		return Optional.ofNullable(throwable);
	}

	public List<Object> values() {
		return sample.values();
	}

	@Override
	public int compareTo(FalsifiedSample o) {
		return sample.compareTo(o.sample);
	}
}
