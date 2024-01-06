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

	private static void printHistogram(int tries, Map<Integer, Integer> counts) {
		SortedSet<Integer> keys = new TreeSet<>(counts.keySet());
		for (Integer key : keys) {
			int count = counts.get(key) / (tries / 4000);
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < count; i++) {
				sb.append("*");
			}
			System.out.printf("%3d: %s%n", key, sb.toString());
		}
	}

}
