package jqwik2;

import java.util.*;
import java.util.stream.*;

public sealed interface Shrinkable<T> extends Comparable<Shrinkable<T>> {
	T value();

	Generator<T> generator();

	ChoicesRecording recording();

	Stream<Shrinkable<T>> shrink();

	default T regenerate() {
		return generator().generate(new RecordedSource(recording()));
	}

	@SuppressWarnings("unchecked")
	default Shrinkable<Object> asGeneric() {
		return (Shrinkable<Object>) this;
	}
}

record GeneratedShrinkable<T>(T value, Generator<T> generator, ChoicesRecording recording) implements Shrinkable<T> {
	@Override
	public Stream<Shrinkable<T>> shrink() {
		return recording.shrink()
						.map(s -> {
							try {
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
