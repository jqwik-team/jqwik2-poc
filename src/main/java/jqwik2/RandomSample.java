package jqwik2;

import java.util.*;

public record RandomSample(
	List<Shrinkable<Object>> shrinkables,
	String seed
) implements Sample {
}
