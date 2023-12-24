package jqwik2;

import java.util.*;

import jqwik2.api.*;

public class PropertyCase {

	private final String seed;
	private final int tries;
	private final double edgeCasesProbability;

	private final List<Generator> generators;
	private final Tryable tryable;

	public PropertyCase(String seed, int tries, double edgeCasesProbability, List<Generator> generators, Tryable tryable) {
		this.seed = seed;
		this.tries = tries;
		this.edgeCasesProbability = edgeCasesProbability;
		this.generators = generators;
		this.tryable = tryable;
	}

	PropertyExecutionResult execute() {
		return PropertyExecutionResult.SUCCESSFUL;
	}

}
