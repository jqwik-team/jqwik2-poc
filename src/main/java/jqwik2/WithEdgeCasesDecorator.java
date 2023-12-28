package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class WithEdgeCasesDecorator<T> extends Generator.Decorator<T> {

	private final double edgeCasesProbability;
	private final int maxEdgeCases;

	private Set<Recording> edgeCasesCache = null;

	public WithEdgeCasesDecorator(Generator<T> generator, double edgeCasesProbability, int maxEdgeCases) {
		super(generator);
		this.edgeCasesProbability = edgeCasesProbability;
		this.maxEdgeCases = maxEdgeCases;
	}

	@Override
	public T generate(GenSource source) {
		return generator.generate(originalOrEdgeCaseSource(source));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof WithEdgeCasesDecorator<?> other) {
			return super.equals(other) &&
					   other.edgeCasesProbability == edgeCasesProbability &&
					   other.maxEdgeCases == maxEdgeCases;
		}
		return false;
	}

	private GenSource originalOrEdgeCaseSource(GenSource originalSource) {
		if (originalSource instanceof GenRecorder recorder) {
			return recorder.swapInnerSource(this::replaceRandomWithEdgeCaseSource);
		}
		return replaceRandomWithEdgeCaseSource(originalSource);
	}

	private GenSource replaceRandomWithEdgeCaseSource(GenSource oldSource) {
		if (oldSource instanceof RandomGenSource randomSource) {
			return randomSource.withProbability(
				edgeCasesProbability,
				() -> edgeCaseSource(randomSource),
				() -> oldSource
			);
		}
		return oldSource;
	}

	private RecordedSource edgeCaseSource(RandomGenSource randomSource) {
		if (edgeCasesCache == null) {
			edgeCasesCache = createEdgeCaseRecordings();
		}
		Recording recording = randomSource.chooseOne(edgeCasesCache);
		return new RecordedSource(recording);
	}

	private Set<Recording> createEdgeCaseRecordings() {
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
