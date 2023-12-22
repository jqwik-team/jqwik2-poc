package jqwik2;

import java.util.*;

public record PropertyExecutionFailure(Sample sample, Throwable exception) {
	public PropertyExecutionFailure(String message) {
		this(null, new AssertionError(message));
	}

	Optional<Sample> failingSample() {
		return Optional.ofNullable(sample);
	}

	Optional<Throwable> thrownException() {
		return Optional.ofNullable(exception);
	}
}
