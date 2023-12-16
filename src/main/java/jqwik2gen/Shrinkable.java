package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public sealed interface Shrinkable<T> extends Comparable<Shrinkable<T>> {
	T value();

	Generator<T> generator();

	SourceRecording recording();

	default T regenerate() {
		return generator().generate(new RecordedSource(recording())).value();
	}

	Stream<Shrinkable<T>> shrink();
}

record Unshrinkable<T>(T value) implements Shrinkable<T> {
	@Override
	public Generator<T> generator() {
		return new ConstantGenerator();
	}

	@Override
	public SourceRecording recording() {
		return new UnshrinkableRecording();
	}

	@Override
	public Stream<Shrinkable<T>> shrink() {
		return Stream.empty();
	}

	@Override
	public int compareTo(Shrinkable<T> o) {
		return this.recording().compareTo(o.recording());
	}

	private class ConstantGenerator implements Generator<T> {
		@Override
		public Shrinkable<T> generate(GenSource source) {
			return Unshrinkable.this;
		}
	}
}

record GeneratedShrinkable<T>(T value, Generator<T> generator, SourceRecording recording) implements Shrinkable<T> {
	@Override
	public Stream<Shrinkable<T>> shrink() {
		return recording.shrink()
						.map(s -> {
							try {
								return generator.generate(new RecordedSource(s));
							} catch (CannotGenerateException e) {
								return null;
							}
						}).filter(Objects::nonNull);
	}

	@Override
	public int compareTo(Shrinkable<T> o) {
		return this.recording().compareTo(o.recording());
	}
}
