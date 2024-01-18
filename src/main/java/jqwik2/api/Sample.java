package jqwik2.api;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.recording.*;
import jqwik2.internal.shrinking.*;

public record Sample(List<Shrinkable<Object>> shrinkables) implements Comparable<Sample> {

	public List<Object> values() {
		return shrinkables().stream()
							.map(Shrinkable::value)
							.toList();
	}

	public List<Object> regenerateValues() {
		return shrinkables().stream().map(Shrinkable::regenerate).toList();
	}

	public Stream<Sample> shrink() {
		return new SampleShrinker(this).shrink();
	}

	public SampleRecording recording() {
		return new SampleRecording(shrinkables.stream().map(Shrinkable::recording).toList());
	}

	@Override
	public int compareTo(Sample other) {
		return this.recording().compareTo(other.recording());
	}

	@Override
	public String toString() {
		return "Sample{%s}".formatted(values().stream().map(Object::toString).toList());
	}

	public int size() {
		return shrinkables().size();
	}
}
