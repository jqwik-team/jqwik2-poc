package jqwik2;

import java.util.*;

public record FalsifiedSample(Sample sample, Throwable exception) {
	Optional<Throwable> thrownException() {
		return Optional.ofNullable(exception);
	}
}
