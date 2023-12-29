package jqwik2;

public sealed interface PropertyRunConfiguration {

	int maxTries();

	boolean shrinkingEnabled();

	static PropertyRunConfiguration randomized(int maxTries) {
		return randomized(null, maxTries);
	}

	static PropertyRunConfiguration randomized(String seed, int maxTries) {
		return randomized(seed, maxTries, true, 0.05);
	}

	static PropertyRunConfiguration randomized(String seed, int maxTries, boolean shrinkingEnabled, double edgeCasesProbability) {
		return new Randomized(seed, maxTries, shrinkingEnabled, edgeCasesProbability);
	}

	record Randomized(String seed, int maxTries, boolean shrinkingEnabled, double edgeCasesProbability) implements PropertyRunConfiguration {
	}
}
