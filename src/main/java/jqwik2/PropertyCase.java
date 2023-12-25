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

	public PropertyCase(List<Generator<?>> generators, Tryable tryable, String seed, int maxTries, double edgeCasesProbability) {
		this.generators = generators;
		this.tryable = tryable;
		this.seed = seed;
		this.maxTries = maxTries;
		this.edgeCasesProbability = edgeCasesProbability;
	}

	PropertyExecutionResult execute() {
		RandomGenSource randomGenSource = new RandomGenSource(seed);
		int countTries = 0;
		int countChecks = 0;

		while(countTries < maxTries) {
			Sample sample = new SampleGenerator(generators).generate(randomGenSource);
			countTries++;
			TryExecutionResult tryResult = tryable.apply(sample);
			if (tryResult.status() != TryExecutionResult.Status.INVALID) {
				countChecks++;
			}
			if (tryResult.status() == TryExecutionResult.Status.FALSIFIED) {
				return new PropertyExecutionResult(FAILED, countTries, countChecks);
			}
		}

		return new PropertyExecutionResult(SUCCESSFUL, countTries, countChecks);
	}

}
