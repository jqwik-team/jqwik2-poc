package jqwik2.api;

import java.util.*;

public interface Exhaustive<T extends Exhaustive<T>> extends Cloneable {
	long maxCount();

	/**
	 * Advance this exhaustive source locally or up the chain.
	 * Return true if successful, false if exhausted.
	 */
	boolean advance();

	void reset();

	T clone();

	default void chain(Exhaustive<?> succ)  {
		this.setSucc(succ);
		succ.setPrev(this);
	}

	/**
	 * Try to advance this exhaustive source with all its successors.
	 * Return true if successful, false if exhausted.
	 */
	boolean advanceChain();

	void setPrev(Exhaustive<?> exhaustive);

	void setSucc(Exhaustive<?> exhaustive);

	Optional<Exhaustive<?>> prev();

	Optional<Exhaustive<?>> succ();
}
