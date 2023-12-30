package jqwik2;

public interface Exhaustive {
	long maxCount();

	void advance();

	void chain(Exhaustive second);

	void next();

	void setBefore(Exhaustive exhaustive);
}
