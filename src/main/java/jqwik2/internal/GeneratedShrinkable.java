package jqwik2.internal;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.recording.*;

public final class GeneratedShrinkable<T> implements Shrinkable<T> {
	private final T value;
	private final Generator<T> generator;
	private final GenRecorder recorder;

	public GeneratedShrinkable(T value, Generator<T> generator, GenRecorder recorder) {
		this.value = value;
		this.generator = generator;
		this.recorder = recorder;
	}

	@Override
	public Stream<Shrinkable<T>> shrink() {
		return recording().shrink()
						  .map(s -> {
							  try {
								  // TODO: Use random backUpSource during shrinking
								  //       Where is the random source coming from?
								  GenRecorder source = new GenRecorder(RecordedSource.of(s));
								  T value = generator.generate(source);
								  return (Shrinkable<T>) new GeneratedShrinkable<>(value, generator, source);
							  } catch (CannotGenerateException e) {
								  return null;
							  }
						  }).filter(Objects::nonNull);
	}

	@Override
	public T value() {
		return value;
	}

	@Override
	public Generator<T> generator() {
		return generator;
	}

	@Override
	public Recording recording() {
		return recorder.recording();
	}

	@Override
	public int compareTo(Shrinkable<T> o) {
		return this.recording().compareTo(o.recording());
	}

	@Override
	public String toString() {
		return "Shrinkable{value=%s, recording=%s}".formatted(value, recording());
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (GeneratedShrinkable<?>) obj;
		return Objects.equals(this.value, that.value) &&
				   Objects.equals(this.generator, that.generator) &&
				   Objects.equals(this.recording(), that.recording());
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, generator, recording());
	}

}
