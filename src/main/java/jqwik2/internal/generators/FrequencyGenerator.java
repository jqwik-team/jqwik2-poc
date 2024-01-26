package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;
import jqwik2.internal.*;

public class FrequencyGenerator<T> implements Generator<T> {

	private final List<Pair<Integer, T>> frequencies;
	private final List<T> values;

	public FrequencyGenerator(Collection<Pair<Integer, ? extends T>> frequencies) {
		this.values = values(frequencies);
		this.frequencies = frequencies.stream()
									  .filter(p -> p.first() > 0)
									  .map(p -> new Pair<>(p.first(), (T) p.second()))
									  .toList();
	}

	private List<T> values(Collection<Pair<Integer, ? extends T>> frequencies) {
		return frequencies.stream()
						  .filter(p -> p.first() > 0)
						  .map(Pair::second)
						  .distinct()
						  .map(v -> (T) v)
						  .toList();
	}

	@Override
	public T generate(GenSource source) {
		if (frequencies.isEmpty()) {
			throw new CannotGenerateException("No values to choose from");
		}
		List<Integer> weights = frequencies.stream().map(Pair::first).toList();
		int index = source.atom().choose(frequencies.size(), weights);
		return frequencies.get(index).second();
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
