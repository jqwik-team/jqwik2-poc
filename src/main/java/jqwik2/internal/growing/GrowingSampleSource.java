package jqwik2.internal.growing;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

/**
 * The implementation is more involved than it may seem necessary at first.
 * There are two reasons for this:
 * <ul>
 *     <li>Prevent generation of duplicate sources if possible.</li>
 *     <li>Generate "grown" sources in smaller batches in order to prevent "pausing"
 *     while generating all sources with size + 1 in one go.</li>
 * </ul>
 */
public class GrowingSampleSource extends SequentialGuidedGeneration implements SampleSource {

	private final Set<GrowingTuple> currentSizeSources = new LinkedHashSet<>();
	private Iterator<GrowingTuple> previousSizeIterator;
	private Iterator<GrowingTuple> currentBatchIterator;
	private GrowingTuple current;

	public GrowingSampleSource() {
		previousSizeIterator = Collections.emptyIterator();
		currentBatchIterator = Collections.emptyIterator();
		current = new GrowingTuple();
	}

	@Override
	public List<GenSource> sources(int size) {
		if (current == null) {
			throw new IllegalStateException("GrowingSampleSource has been exhausted.");
		}
		currentSizeSources.add(current);
		List<GenSource> sources = new ArrayList<>();
		while (sources.size() < size) {
			sources.add(current.nextValue());
		}
		return sources;
	}

	@Override
	protected SampleSource initialSource() {
		return this;
	}

	@Override
	protected SampleSource nextSource() {
		return this;
	}

	@Override
	protected boolean handleResult(TryExecutionResult result, Sample sample) {
		return grow();
	}

	@Override
	protected boolean handleEmptyGeneration(SampleSource failingSource) {
		return grow();
	}

	private boolean grow() {
		while (currentBatchIterator.hasNext()) {
			var next = currentBatchIterator.next();
			if (currentSizeSources.contains(next)) {
				continue;
			}
			current = next;
			return true;
		}
		while (previousSizeIterator.hasNext()) {
			currentBatchIterator = previousSizeIterator.next().grow().iterator();
			if (currentBatchIterator.hasNext()) {
				return grow();
			}
		}
		previousSizeIterator = new ArrayList<>(currentSizeSources).iterator();
		currentSizeSources.clear();
		currentBatchIterator = Collections.emptyIterator();
		if (!previousSizeIterator.hasNext()) {
			return false;
		}
		return grow();
	}
}
