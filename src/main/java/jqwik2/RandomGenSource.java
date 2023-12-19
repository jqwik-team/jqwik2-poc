package jqwik2;

import java.util.*;

public final class RandomGenSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	private final Random random;

	public RandomGenSource() {
		this.random = new Random();
	}

	public RandomGenSource(long seed) {
		this.random = new Random(seed);
	}

	@Override
	public int choice(int max) {
		return random.nextInt(max);
	}

	@Override
	public GenSource nextElement() {
		return new RandomGenSource(random.nextLong());
	}

	@Override
	public Atom atom() {
		return this;
	}

	@Override
	public List list() {
		return this;
	}

	@Override
	public Tree tree() {
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GenSource> T nextElement(Class<T> sourceType) {
		return (T) new RandomGenSource(random.nextLong());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GenSource> T head(Class<T> sourceType) {
		return (T) new RandomGenSource(random.nextLong());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GenSource> T child(Class<T> sourceType) {
		return (T) new RandomGenSource(random.nextLong());
	}
}
