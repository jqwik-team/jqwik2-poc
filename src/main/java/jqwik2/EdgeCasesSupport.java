package jqwik2;

import java.util.stream.*;

class EdgeCasesSupport {
	public static Iterable<GenSource> chooseInt(int min, int max) {
		return GenSourceSupport.chooseIntEdgeCases(min, max)
				   .stream()
				   .map(RecordedSource::new).collect(Collectors.toSet());
	}
}
