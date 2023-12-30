package jqwik2.api;

import java.util.*;

/**
 * A multi gen source is used to provide gen sources for more than one parameter.
 */
public interface MultiGenSource {

	static MultiGenSource of(GenSource...sources) {
		return new SimpleMultiSource(List.of(sources));
	}

	List<GenSource> sources(int size);

	class SimpleMultiSource implements MultiGenSource {
		private final List<GenSource> sources;

		public SimpleMultiSource(List<GenSource> sources) {
			this.sources = sources;
		}

		@Override
		public List<GenSource> sources(int size) {
			if (size != sources.size()) {
				String message = "Expected %d sources, but got %d".formatted(size, sources.size());
				throw new IllegalArgumentException(message);
			}
			return sources;
		}
	}
}
