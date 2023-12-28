package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

class WithEdgeCasesDecorator extends Generator.Decorator<Object> {

	private final double edgeCasesProbability;
	private final int maxEdgeCases;

	private Set<Recording> edgeCasesCache = null;

	WithEdgeCasesDecorator(Generator<Object> generator, double edgeCasesProbability, int maxEdgeCases) {
		super(generator);
		this.edgeCasesProbability = edgeCasesProbability;
		this.maxEdgeCases = maxEdgeCases;
	}

	@Override
	public Object generate(GenSource source) {
		return generator.generate(chooseRandomOrEdgeCaseSource(source));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WithEdgeCasesDecorator other) {
			return super.equals(other) &&
					   other.edgeCasesProbability == edgeCasesProbability &&
					   other.maxEdgeCases == maxEdgeCases;
		}
		return false;
	}

	private GenSource chooseRandomOrEdgeCaseSource(GenSource originalSource) {
		if (originalSource instanceof GenRecorder recorder) {
			return recorder.swapInnerSource(
				oldSource -> {
					if (oldSource instanceof RandomGenSource randomSource) {
						return randomSource.withProbability(
							edgeCasesProbability,
							() -> edgeCaseSource(randomSource),
							() -> oldSource
						);
					}
					return oldSource;
				}
			);
		}
		return originalSource;
	}

	private RecordedSource edgeCaseSource(RandomGenSource randomSource) {
		if (edgeCasesCache == null) {
			edgeCasesCache = createEdgeCaseRecordings(generator);
		}
		Recording recording = randomSource.chooseOne(edgeCasesCache);
		return new RecordedSource(recording);
	}

	private Set<Recording> createEdgeCaseRecordings(Generator<Object> generator) {
		LinkedHashSet<Recording> edgeCases = new LinkedHashSet<>();
		Iterator<Recording> iterator = generator.edgeCases().iterator();
		while (edgeCases.size() < maxEdgeCases) {
			if (!iterator.hasNext()) {
				break;
			}
			edgeCases.add(iterator.next());
		}
		return edgeCases;
	}
}
