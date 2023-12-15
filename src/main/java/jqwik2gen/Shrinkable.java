package jqwik2gen;

public sealed interface Shrinkable<T> {
	T value();

	Generator<T> generator();

	RecordedSource source();

	default T regenerate() {
		return generator().generate(new ReproducingSource(source())).value();
	}
}

record Unshrinkable<T>(T value) implements Shrinkable<T> {
	@Override
	public Generator<T> generator() {
		return new ConstantGenerator();
	}

	@Override
	public RecordedSource source() {
		return new UnshrinkableSource();
	}

	private class ConstantGenerator implements Generator<T> {
		@Override
		public Shrinkable<T> generate(GenSource source) {
			return Unshrinkable.this;
		}
	}
}

record GeneratedShrinkable<T>(T value, Generator<T> generator, RecordedSource source) implements Shrinkable<T> {
}
