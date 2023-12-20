package jqwik2;

public interface Generator<T> {

	T generate(GenSource source);
}
