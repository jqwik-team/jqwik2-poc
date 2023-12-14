package jqwik2gen;

public interface Generator<T> {
	Shrinkable<T> generate(GenSource source);
}
