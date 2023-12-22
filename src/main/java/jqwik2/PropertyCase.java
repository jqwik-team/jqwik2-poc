package jqwik2;

import java.util.*;

public class PropertyCase {

	private final String seed;
	private final int tries;
	private final double edgeCasesProbability;

	private final List<Generator> generators;
	private final PropertyCode propertyCode;

	public PropertyCase(String seed, int tries, double edgeCasesProbability, List<Generator> generators, PropertyCode propertyCode) {
		this.seed = seed;
		this.tries = tries;
		this.edgeCasesProbability = edgeCasesProbability;
		this.generators = generators;
		this.propertyCode = propertyCode;
	}

	PropertyExecutionResult execute() {
		return PropertyExecutionResult.SUCCESSFUL;
	}

}
