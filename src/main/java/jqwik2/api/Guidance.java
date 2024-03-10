package jqwik2.api;

import java.util.*;

import jqwik2.api.validation.*;
import jqwik2.internal.*;

/**
 * Must be implemented thread safe since it might be used from multiple threads.
 */
public interface Guidance {

	Guidance NULL = new Guidance() {
		@Override
		public void guide(TryExecutionResult result, Sample sample) {
		}
	};

	/**
	 * Guides by feeding the result of a property try.
	 *
	 * <p>Will be called after each try</p>
	 */
	void guide(TryExecutionResult result, Sample sample);

	/**
	 * Called when a sample source will not be able to generate a valid sample.
	 * <p>
	 * Especially sequential guided generation will have to use this to trigger next generation step.
	 */
	default void onEmptyGeneration(SampleSource failingSource) {
	}

	/**
	 * Called when a property run has finished.
	 * <p>
	 * Can be used to change validation status and failure exception.
	 */
	default Optional<Pair<PropertyValidationStatus, Throwable>> overrideValidationStatus(PropertyValidationStatus status) {
		return Optional.empty();
	}

	default void stop() {
	}
}
