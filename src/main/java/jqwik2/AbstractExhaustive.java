package jqwik2;

public abstract class AbstractExhaustive implements Exhaustive {

	protected Exhaustive succ = null;
	protected Exhaustive prev = null;

	@Override
	public void next() {
		if (succ == null) {
			advance();
		} else {
			succ.next();
		}
	}

	@Override
	public void setPrev(Exhaustive exhaustive) {
		this.prev = exhaustive;
	}

	@Override
	public void setSucc(Exhaustive exhaustive) {
		this.succ = exhaustive;
	}
}
