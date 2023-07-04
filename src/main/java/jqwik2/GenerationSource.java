package jqwik2;

public interface GenerationSource {
	int[] next(int count);

	// GenerationStream next(int count, int min, int max);
	// GenerationStream next(int count, int min, int max, GenerationStream parent);
}
