package jqwik2.recording;

import java.util.*;
import java.util.stream.*;

public sealed interface ChoicesRecording extends Comparable<ChoicesRecording>
	permits AtomRecording, ListRecording, TreeRecording {

	Stream<? extends ChoicesRecording> shrink();

	static Builder builder() {
		return new Builder();
	}

	class Builder {
		public AtomRecording atom(Integer... choices) {
			return new AtomRecording(choices);
		}
	}
}

// TODO: Tuples could have more strict shrinking, since elements cannot be exchanged and size is stable
// record TupleSource(List<SourceRecording> tuple) implements SourceRecording {
// 	@Override
// 	public Collection<SourceRecording> children() {
// 		return Collections.emptyList();
// 	}
// }

