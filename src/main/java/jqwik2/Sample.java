package jqwik2;

import java.util.*;

public interface Sample {
	List<Shrinkable<Object>> shrinkables();

	default List<Object> values() {
		return shrinkables().stream()
							.map(Shrinkable::value)
							.toList();
	}
}
