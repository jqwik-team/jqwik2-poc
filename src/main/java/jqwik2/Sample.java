package jqwik2;

import java.util.*;
import java.util.stream.*;

public record Sample(List<Shrinkable<Object>> shrinkables) implements Comparable<Sample> {

	public List<Object> values() {
		return shrinkables().stream()
							.map(Shrinkable::value)
							.toList();
	}

	List<Object> regenerateValues() {
		return shrinkables().stream().map(Shrinkable::regenerate).toList();
	}


	public Stream<Sample> shrink() {
		if (size() != 1) {
			throw new IllegalArgumentException("Only one parameter supported for now!");
		}
		Shrinkable<Object> firstParam = shrinkables().getFirst();
		return firstParam.shrink().map((Shrinkable<Object> s) -> new Sample(List.of(s)));
	}

	@Override
	public int compareTo(Sample o) {
		if (size() != 1) {
			throw new IllegalArgumentException("Only one parameter supported for now!");
		}
		return shrinkables().getFirst().compareTo(o.shrinkables().getFirst());
	}

	@Override
	public String toString() {
		return "Sample{%s}".formatted(values().stream().map(Object::toString).toList());
	}

	int size() {
		return shrinkables().size();
	}
}
