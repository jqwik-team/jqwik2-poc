package jqwik2;

public interface Exhaustive {
	long maxCount();

	void advance();

	default void chain(Exhaustive succ)  {
		this.setSucc(succ);
		succ.setPrev(this);
	}

	void next();

	void setPrev(Exhaustive exhaustive);

	void setSucc(Exhaustive exhaustive);
}
