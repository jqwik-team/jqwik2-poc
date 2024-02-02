package jqwik2.internal.growing;

class GrowingGenSource extends AbstractGrowingSource {

	private GrowingSourceContainer currentSource = new GrowingSourceContainer();

	@Override
	public Atom atom() {
		return currentSource.get(Atom.class, GrowingAtom::new);
	}

	public boolean advance() {
		return currentSource.advance();
	}

	public void reset() {
		currentSource.reset();
	}

}
