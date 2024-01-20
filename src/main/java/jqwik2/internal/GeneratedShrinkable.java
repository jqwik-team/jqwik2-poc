package jqwik2.internal;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.recording.*;

public record GeneratedShrinkable<T>(T value, Generator<T> generator, Recording recording) implements Shrinkable<T> {
	@Override
	public Stream<Shrinkable<T>> shrink() {
		return recording.shrink()
						.map(s -> {
							try {
								// TODO: Use random backUpSource during shrinking
								//       Where is the random source coming from?
								GenRecorder source = new GenRecorder(RecordedSource.of(s));
								T value = generator.generate(source);
								return (Shrinkable<T>) new GeneratedShrinkable<>(value, generator, source.recording());
							} catch (CannotGenerateException e) {
								return null;
							}
						}).filter(Objects::nonNull);
	}

	@Override
	public int compareTo(Shrinkable<T> o) {
		return this.recording().compareTo(o.recording());
	}

	@Override
	public String toString() {
		return "Shrinkable{value=%s, recording=%s}".formatted(value, recording);
	}
}
