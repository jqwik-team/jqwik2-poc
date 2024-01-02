package jqwik2.api;

import java.util.*;

public interface Exhaustive<T extends Exhaustive<T>> extends Cloneable {
	long maxCount();

	void advance();

	void reset();

	T clone();

	default void chain(Exhaustive<?> succ)  {
		this.setSucc(succ);
		succ.setPrev(this);
	}

	void next();

	void setPrev(Exhaustive<?> exhaustive);

	void setSucc(Exhaustive<?> exhaustive);

	Optional<Exhaustive<?>> prev();

	Optional<Exhaustive<?>> succ();
}
