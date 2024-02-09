package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;
import jqwik2.internal.recording.*;

import static jqwik2.api.recording.Recording.*;

public class FrequencyOfGenerator<T> implements Generator<T> {

	private final Generator<Generator<T>> generatorFrequency;
	private final List<Pair<Recording, Generator<T>>> sourceAndGenerators;

	public FrequencyOfGenerator(Collection<Pair<Integer, Generator<T>>> frequencies) {
		this.generatorFrequency = BaseGenerators.frequency(frequencies);
		this.sourceAndGenerators = sourceAndGenerators(generatorFrequency);
	}

	private List<Pair<Recording, Generator<T>>> sourceAndGenerators(Generator<Generator<T>> generatorFrequency) {
		// TODO: This is involved. Should there be an easier way to get all values and their recordings?
		List<Pair<Recording, Generator<T>>> sourceAndGenerators = new ArrayList<>();
		generatorFrequency.exhaustive().ifPresent(exhaustive -> {
			for (GenSource genSource : exhaustive) {
				GenRecorder recorder = new GenRecorder(genSource);
				Generator<T> generator = generatorFrequency.generate(recorder);
				sourceAndGenerators.add(Pair.of(recorder.recording(), generator));
			}
		});
		return sourceAndGenerators;
	}

	@Override
	public T generate(GenSource source) {
		return generatorFrequency.flatMap(g -> g).generate(source);
	}

	@Override
	public Iterable<Recording> edgeCases() {
		// TODO: make sure this does not run infinitely in highly nested cases
		Set<Recording> recordings = new LinkedHashSet<>();
		for (Pair<Recording, Generator<T>> sourceAndGenerator : sourceAndGenerators) {
			Recording sourceRecording = sourceAndGenerator.first();
			Generator<T> generator = sourceAndGenerator.second();
			for (Recording childRecording : generator.edgeCases()) {
				recordings.add(tuple(
					sourceRecording,
					childRecording
				));
			}
		}
		return recordings;
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return generatorFrequency.flatMap(g -> g).exhaustive();
	}

}
