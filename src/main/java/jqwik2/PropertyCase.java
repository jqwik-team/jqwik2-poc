package jqwik2;

import java.util.*;

import jqwik2.api.*;

import static jqwik2.api.PropertyExecutionResult.Status.*;

public class PropertyCase {

	private final String seed;
	private final int tries;
	private final double edgeCasesProbability;

	private final List<Generator> generators;
	private final Tryable tryable;

	public PropertyCase(List<Generator> generators, Tryable tryable, String seed, int tries, double edgeCasesProbability) {
		this.seed = seed;
		this.tries = tries;
		this.edgeCasesProbability = edgeCasesProbability;
		this.generators = generators;
		this.tryable = tryable;
	}

	PropertyExecutionResult execute() {
		return new PropertyExecutionResult(SUCCESSFUL, 0, 0);
	}

}
