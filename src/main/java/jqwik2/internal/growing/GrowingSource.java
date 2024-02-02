package jqwik2.internal.growing;

import jqwik2.api.*;

public interface GrowingSource {
	boolean advance();
	void reset();
	void next();
}
