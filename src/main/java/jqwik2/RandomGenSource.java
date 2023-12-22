package jqwik2;

public final class RandomGenSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	private final RandomChoice random;

	public RandomGenSource() {
		this(RandomChoice.create());
	}

	public RandomGenSource(String seed) {
		this(RandomChoice.create(seed));
	}

	public RandomGenSource(RandomChoice random) {
		this.random = random;
	}


	@Override
	public int choose(int maxExcluded) {
		return random.nextInt(maxExcluded);
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
	public GenSource nextElement() {
		return new RandomGenSource(random.split());
	}

	@Override
	public GenSource head() {
		return new RandomGenSource(random.split());
	}

	@Override
	public GenSource child() {
		return new RandomGenSource(random.split());
	}

	/**
	 * Split off a new random gen source for usage in different generators
	 * @return a new random gen source
	 */
	public RandomGenSource split() {
		return new RandomGenSource(random.split());
	}
}
