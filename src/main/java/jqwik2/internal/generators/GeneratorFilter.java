package jqwik2.internal.generators;

import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;
import jqwik2.internal.recording.*;

public class GeneratorFilter<T> implements Generator<T> {

	private final Generator<T> generator;
	private final Predicate<T> filter;
	private final int maxMisses;

	public GeneratorFilter(Generator<T> generator, Predicate<T> filter, int maxMisses) {
		this.generator = generator;
		this.filter = filter;
		this.maxMisses = maxMisses;
	}

	@Override
	public T generate(GenSource source) {
		return generateUntilAccepted(source);
	}

	@Override
	public Iterable<Recording> edgeCases() {
		var unfilteredEdgeCases = generator.edgeCases();
		return () -> new FilteredRecordingIterator(
			unfilteredEdgeCases.iterator(),
			recording -> generator.fromRecording(recording)
								  .map(filter::test).orElse(false)
		);
	}

	private T generateUntilAccepted(GenSource source) {
		for (int i = 0; i < maxMisses; i++) {
			try {
				T value = generator.generate(source);
				if (filter.test(value)) {
					return value;
				}
			} catch (CannotGenerateException ignore) {}
		}
		String message = String.format("%s missed more than %s times.", toString(), maxMisses);
		throw new TooManyFilterMissesException(message);
	}
}
