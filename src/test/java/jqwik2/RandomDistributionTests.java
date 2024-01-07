package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

import net.jqwik.api.*;

class RandomDistributionTests {

	RandomChoice randomChoice = RandomChoice.create();

	@Example
	void gaussian() {
		RandomChoice.Distribution gaussianDistribution = new GaussianDistribution(2.8);
		Map<Integer, Integer> counts = new HashMap<>();
		var tries = 10000000;
		for (int i = 0; i < tries; i++) {
			int gaussian = randomChoice.nextInt(100, gaussianDistribution);
			counts.compute(gaussian, (k, v) -> v == null ? 1 : v + 1);
		}

		// printHistogram(tries, counts);
	}

	@Example
	void biased() {
		RandomChoice.Distribution gaussianDistribution = new BiasedDistribution(5);
		Map<Integer, Integer> counts = new HashMap<>();
		var tries = 1000000;
		for (int i = 0; i < tries; i++) {
			var maxExcluded = Integer.MAX_VALUE;
			int gaussian = randomChoice.nextInt(maxExcluded, gaussianDistribution);
			// Group to 100 groups
			int groupFactor = maxExcluded / 100;
			int key = gaussian / groupFactor;
			counts.compute(key, (k, v) -> v == null ? 1 : v + 1);
		}

		printHistogram(tries, counts);
	}

	private static void printHistogram(int tries, Map<Integer, Integer> counts) {
		SortedSet<Integer> keys = new TreeSet<>(counts.keySet());
		int maxValue = new TreeSet<>(counts.values()).last();
		var scaleDown = maxValue / 100;
		for (Integer key : keys) {
			int count = counts.get(key);
			int prints = count / scaleDown;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < prints; i++) {
				sb.append("*");
			}
			System.out.printf("%3d [%6d]: %s%n", key, count, sb);
		}
	}

}
