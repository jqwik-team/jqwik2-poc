package jqwik2;

import java.util.*;

public record FalsifiedSample(Sample sample, Throwable throwable) {
	Optional<Throwable> thrown() {
		return Optional.ofNullable(throwable);
	}
}
