package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;

/**
 * This works sequentially only since GrowingSampleSource is a sequential guidance
 */
public class IterableGrowingSource implements IterableSampleSource {
	@Override
	public boolean stopWhenFalsified() {
		return true;
	}

	@Override
	public Iterator<SampleSource> iterator() {
		return new GrowingSampleSource();
	}
}
