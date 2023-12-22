package jqwik2;

import jqwik2.recording.*;

public interface Generator<T> {

	T generate(GenSource source);

	default GenSource edgeCases() {
		return new RecordedSource(Recording.atom());
	}
}
