package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;
import jqwik2.internal.*;

class ChooseGenerator<T> implements Generator<T> {
	private final List<T> values;

	ChooseGenerator(Collection<? extends T> values) {
		this.values = new ArrayList<>(values);
	}

	@Override
	public T generate(GenSource source) {
		if (values.isEmpty()) {
			throw new CannotGenerateException("No values to choose from");
		}
		int index = source.atom().choose(values.size());
		return values.get(index);
	}

	@Override
	public Iterable<Recording> edgeCases() {
		return EdgeCasesSupport.forAtom(values.size() - 1);
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.atom(values.size() - 1);
	}
}
