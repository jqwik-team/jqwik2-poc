package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;
import jqwik2.internal.*;

import static jqwik2.api.recording.Recording.*;

public class FrequencyOfGenerator<T> implements Generator<T> {

	private final List<Pair<Integer, Generator<T>>> frequencies;
	private final List<Generator<T>> values;
	private final RandomChoice.Distribution distribution;

	public FrequencyOfGenerator(Collection<Pair<Integer, Generator<T>>> frequencies) {
		this.values = values(frequencies);
		this.frequencies = frequencies.stream()
									  .filter(p -> p.first() > 0)
									  .map(p -> new Pair<>(p.first(), p.second()))
									  .toList();
		this.distribution = frequencyDistribution();
	}

	private List<Generator<T>> values(Collection<Pair<Integer, Generator<T>>> frequencies) {
		return frequencies.stream()
						  .filter(p -> p.first() > 0)
						  .map(Pair::second)
						  .distinct()
						  .toList();
	}

	@Override
	public T generate(GenSource source) {
		if (frequencies.isEmpty()) {
			throw new CannotGenerateException("No values to choose from");
		}
		var frequencyOfSource = source.tuple();
		var generator = chooseGenerator(frequencyOfSource.nextValue());
		return generator.generate(frequencyOfSource.nextValue());
	}

	private Generator<T> chooseGenerator(GenSource chooseGeneratorSource) {
		int index = chooseGeneratorSource.choice().choose(frequencies.size(), distribution);
		return frequencies.get(index).second();
	}

	private RandomChoice.Distribution frequencyDistribution() {
		return new FrequencyBasedDistribution(frequencies);
	}

	@Override
	public Iterable<Recording> edgeCases() {
		// TODO: make sure this does not run infinitely in highly nested cases
		Set<Recording> recordings = new LinkedHashSet<>();
		for (int index = 0; index < values.size(); index++) {
			Generator<T> generator = values.get(index);
			for (Recording edgeCase : generator.edgeCases()) {
				recordings.add(tuple(
					Recording.choice(index),
					edgeCase
				));
			}
		}
		return recordings;

	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.flatMap(
			ExhaustiveSource.choice(values.size() - 1),
			head -> chooseGenerator(head).exhaustive()
		);
	}

}
