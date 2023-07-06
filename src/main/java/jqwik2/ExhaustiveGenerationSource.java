package jqwik2;

public class ExhaustiveGenerationSource implements GenerationSource {

	private int next = 0;

	@Override
	public int[] next(int count, int min, int max) {
		if (count > 1) throw new IllegalArgumentException("Exhaustive generation only supports count == 1");
		return new int[0];
	}
}
