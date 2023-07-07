package jqwik2;

public interface ValueGenerator<T> {

	GeneratedValue<T> generate(GenerationSource source);
}
