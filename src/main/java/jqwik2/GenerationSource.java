package jqwik2;

public interface GenerationSource {
	int[] next(int count, int min, int max);
}