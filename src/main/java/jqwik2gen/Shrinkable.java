package jqwik2gen;

public sealed interface Shrinkable<T> {
	T value();

	Generator<T> generator();

	SourceRecording source();

	default T regenerate() {
		return generator().generate(new RecordedSource(source())).value();
	}
}

record Unshrinkable<T>(T value) implements Shrinkable<T> {
	@Override
	public Generator<T> generator() {
		return new ConstantGenerator();
	}

	@Override
	public SourceRecording source() {
		return new UnshrinkableRecording();
	}

	private class ConstantGenerator implements Generator<T> {
		@Override
		public Shrinkable<T> generate(GenSource source) {
			return Unshrinkable.this;
		}
	}
}

record GeneratedShrinkable<T>(T value, Generator<T> generator, SourceRecording source) implements Shrinkable<T> {
}
