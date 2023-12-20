package jqwik2;

import java.util.*;

public record Sample(List<Shrinkable<Object>> shrinkables) {

	public List<Object> values() {
		return shrinkables().stream()
							.map(Shrinkable::value)
							.toList();
	}

	List<Object> regenerateValues() {
		return shrinkables().stream().map(Shrinkable::regenerate).toList();
	}
}
