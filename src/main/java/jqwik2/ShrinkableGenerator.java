package jqwik2;

import jqwik2.api.*;

public class ShrinkableGenerator<T> {
	private final Generator<T> generator;

	public ShrinkableGenerator(Generator<T> generator) {
		this.generator = generator;
	}

	public Shrinkable<T> generate(GenSource source) {
		GenRecorder recorder = new GenRecorder(source);
		T value = generator.generate(recorder);
		return new GeneratedShrinkable<>(value, generator, recorder.recording());
	}
}
