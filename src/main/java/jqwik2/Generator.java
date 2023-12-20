package jqwik2;

public interface Generator<T> {
	Shrinkable<T> generate_OLD(GenSource source);

	T generate(GenSource source);
}
