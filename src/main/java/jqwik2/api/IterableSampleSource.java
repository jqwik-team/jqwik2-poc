package jqwik2.api;

/**
 * An iterable gen source is used to provide sample gen sources for more than one try.
 */
public interface IterableSampleSource extends Iterable<SampleSource>, ThreadSafety {

	// TODO: Should that method be part of SampleSource or Guidance instead?
	//       Or it could be a configuration value in PropertyRunConfiguration
	default boolean stopWhenFalsified() {
		return true;
	}
}
