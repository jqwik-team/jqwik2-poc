package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.recording.*;

record GeneratedShrinkable<T>(T value, Generator<T> generator, Recording recording) implements Shrinkable<T> {
	@Override
	public Stream<Shrinkable<T>> shrink() {
		return recording.shrink()
						.map(s -> {
							try {
								// TODO: Use random backUpSource during shrinking
								//       Where is the random source coming from?
								GenRecorder source = new GenRecorder(new RecordedSource(s));
								T value = generator.generate(source);
								return (Shrinkable<T>) new GeneratedShrinkable<>(value, generator, s);
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
