package jqwik2;

import java.util.*;

public record TryExecutionFailure(Sample sample, Throwable exception) {
	public TryExecutionFailure(String message) {
		this(null, new AssertionError(message));
	}

	Optional<Sample> failingSample() {
		return Optional.ofNullable(sample);
	}

	Optional<Throwable> thrownException() {
		return Optional.ofNullable(exception);
	}
}
