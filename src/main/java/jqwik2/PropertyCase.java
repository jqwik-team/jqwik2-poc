package jqwik2;

import java.util.*;

import jqwik2.api.*;

import static jqwik2.api.PropertyExecutionResult.Status.*;

public class PropertyCase {
	private final List<Generator<?>> generators;
	private final Tryable tryable;

	private final String seed;
	private final int maxTries;
	private final double edgeCasesProbability;
	private final boolean shrinkingEnabled;

	public PropertyCase(
		List<Generator<?>> generators, Tryable tryable,
		String seed, int maxTries, double edgeCasesProbability, boolean shrinkingEnabled
	) {
		this.generators = generators;
		this.tryable = tryable;
		this.seed = seed;
		this.maxTries = maxTries;
		this.edgeCasesProbability = edgeCasesProbability;
		this.shrinkingEnabled = shrinkingEnabled;
	}

	PropertyExecutionResult execute() {
		RandomGenSource randomGenSource = new RandomGenSource(seed);
		SampleGenerator sampleGenerator = new SampleGenerator(generators);

		int countTries = 0;
		int countChecks = 0;

		while (countTries < maxTries) {
			Sample sample = sampleGenerator.generate(randomGenSource);
			countTries++;
			TryExecutionResult tryResult = tryable.apply(sample);
			if (tryResult.status() != TryExecutionResult.Status.INVALID) {
				countChecks++;
			}
			if (tryResult.status() == TryExecutionResult.Status.FALSIFIED) {
				SortedSet<FalsifiedSample> falsifiedSamples = new TreeSet<>();
				FalsifiedSample originalSample = new FalsifiedSample(sample, tryResult.throwable());
				falsifiedSamples.add(originalSample);
				shrink(originalSample, falsifiedSamples);
				return new PropertyExecutionResult(
					FAILED, countTries, countChecks,
					falsifiedSamples
				);
			}
		}

		return new PropertyExecutionResult(SUCCESSFUL, countTries, countChecks);
	}

	private void shrink(FalsifiedSample originalSample, Collection<FalsifiedSample> falsifiedSamples) {
		if (!shrinkingEnabled) {
			return;
		}
		new FullShrinker(originalSample, tryable).shrinkToEnd(
				falsifiedSamples::add
		);
	}

}
