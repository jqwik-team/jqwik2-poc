package jqwik2.internal.growing;

import jqwik2.api.*;

public interface GrowingSource extends GenSource {
	boolean advance();
	void reset();
}
