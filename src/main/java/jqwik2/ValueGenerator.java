package jqwik2;

public interface ValueGenerator<T> {
	T value(GenerationSource source);
}
