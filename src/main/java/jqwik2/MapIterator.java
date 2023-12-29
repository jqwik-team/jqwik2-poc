package jqwik2;

import java.util.*;
import java.util.function.*;

public class MapIterator<S, T> implements Iterator<T> {

	private final Iterator<S> source;
	private final Function<S, T> mapper;

	public MapIterator(Iterator<S> source, Function<S, T> mapper) {
		this.source = source;
		this.mapper = mapper;
	}

	@Override
	public boolean hasNext() {
		return source.hasNext();
	}

	@Override
	public T next() {
		return mapper.apply(source.next());
	}
}
