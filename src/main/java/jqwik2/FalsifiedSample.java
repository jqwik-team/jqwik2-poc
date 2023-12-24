package jqwik2;

import java.util.*;

import jqwik2.api.*;

public record FalsifiedSample(Sample sample, Throwable throwable) {
	Optional<Throwable> thrown() {
		return Optional.ofNullable(throwable);
	}
}
