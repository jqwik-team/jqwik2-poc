package jqwik2;

public interface Generator<T> {
	Shrinkable<T> generate(GenSource source);
}
