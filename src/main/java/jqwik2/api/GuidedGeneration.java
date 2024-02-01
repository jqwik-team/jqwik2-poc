package jqwik2.api;

import java.util.*;

/**
 * Must be implemented thread safe since it might be used from multiple threads.
 */
public interface GuidedGeneration extends Iterator<SampleSource> {

	/**
	 * Decide if another sample can be tried.
	 * <p>
	 * Method could potentially block to wait for guiding algorithm to finish.
	 * <p>
	 * If it returns false generation will be finished.
	 */
	@Override
	boolean hasNext();

	/**
	 * Returns a reference to a gen source for the next try.
	 *
	 * @throws NoSuchElementException if there is no next try available
	 */
	@Override
	SampleSource next();

	/**
	 * Guides by feeding the result of a property try.
	 *
	 * <p>Will be called after each try</p>
	 */
	void guide(TryExecutionResult result, Sample sample);

	/**
	 * Called when a property run has finished.
	 * <p>
	 * Can be used to override the result of a property run.
	 */
	default PropertyRunResult overridePropertyResult(PropertyRunResult originalResult) {
		return originalResult;
	}
}
