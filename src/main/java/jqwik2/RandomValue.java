package jqwik2;

public interface RandomValue<T> {
	T value(RandomSource source);
}
