package jqwik2.api;

/**
 * An iterable gen source is used to provide sample gen sources for more than one try.
 */
public interface IterableSampleSource extends Iterable<SampleSource>, ThreadSafety {
}
