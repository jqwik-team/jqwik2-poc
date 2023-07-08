package jqwik2;

import java.util.*;

public class FixedGenerationSource implements GenerationSource {
	private final List<Integer> seeds;
	private int position = 0;

	public FixedGenerationSource(List<Integer> seeds) {
		this.seeds = seeds;
	}

	@Override
	public int[] next(int count, int min, int max) {
		int[] ints = new int[count];
		for (int i = 0; i < count; i++) {
			ints[i] = seeds.get(position++);
			if (position >= seeds.size()) {
				position = 0;
			}
		}
		return ints;
	}
}
