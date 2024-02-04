package jqwik2.api;

import java.util.*;

/**
 * Must be implemented thread safe since it might be used from multiple threads.
 */
public interface GuidedGeneration extends Iterator<SampleSource>, Guidance {
}
