package jqwik2.api;

import java.util.*;

/**
 * A multi gen source is used to provide gen sources for more than one parameter,
 * but just a single sample.
 */
public interface SampleSource {

	static SampleSource of(GenSource...sources) {
		return new SimpleMultiSource(List.of(sources));
	}

	static SampleSource of(List<? extends GenSource> sources) {
		return new SimpleMultiSource(sources);
	}

	List<GenSource> sources(int size);

	class SimpleMultiSource implements SampleSource {
		private final List<? extends GenSource> sources;

		public SimpleMultiSource(List<? extends GenSource> sources) {
			this.sources = sources;
		}

		@SuppressWarnings("unchecked")
		@Override
		public List<GenSource> sources(int size) {
			if (size != sources.size()) {
				String message = "Expected %d sources, but got %d".formatted(size, sources.size());
				throw new IllegalArgumentException(message);
			}
			return (List<GenSource>) sources;
		}
	}
}
