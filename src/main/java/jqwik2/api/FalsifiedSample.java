package jqwik2.api;

import java.util.*;

public record FalsifiedSample(Sample sample, Throwable throwable) implements Comparable<FalsifiedSample> {
	public Optional<Throwable> thrown() {
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
