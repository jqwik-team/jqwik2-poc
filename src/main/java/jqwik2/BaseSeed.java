package jqwik2;

public record BaseSeed(int count, int min, int max, int[] values) implements Seed {
	@Override
	public GenerationSource get() {
		return new GenerationSource() {
			@Override
			public int[] next(int count, int min, int max) {
				return values;
			}
		};
	}
}
