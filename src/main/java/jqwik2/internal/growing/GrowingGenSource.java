package jqwik2.internal.growing;

class GrowingGenSource extends AbstractGrowingSource {

	private GrowingSourceContainer currentSource = new GrowingSourceContainer();

	@Override
	public Atom atom() {
		return currentSource.get(Atom.class, GrowingAtom::new);
	}

	@Override
	public Tuple tuple() {
		return currentSource.get(Tuple.class, GrowingTuple::new);
	}

	@Override
	public List list() {
		return currentSource.get(List.class, GrowingList::new);
	}

	@Override
	public boolean advance() {
		return currentSource.advance();
	}

	@Override
	public void reset() {
		currentSource.reset();
	}

	@Override
	public void next() {
		currentSource.next();
	}

}
