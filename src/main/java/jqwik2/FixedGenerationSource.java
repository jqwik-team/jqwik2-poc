package jqwik2;

public class FixedGenerationSource implements GenerationSource {
	private final int[] stream;
	private int position = 0;

	public FixedGenerationSource(int... stream) {
		this.stream = stream;
	}

	@Override
	public int[] next(int count, int min, int max) {
		int[] ints = new int[count];
		for (int i = 0; i < count; i++) {
			ints[i] = stream[position++];
			if (position >= stream.length) {
				position = 0;
			}
		}
		return ints;
	}
}
