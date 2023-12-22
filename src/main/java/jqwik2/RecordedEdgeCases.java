package jqwik2;

import java.util.*;

import jqwik2.recording.*;

class RecordedEdgeCases implements GenSource {
	private final Iterator<Recording> recordings;

	public RecordedEdgeCases(java.util.List<Recording> recordings) {
		this.recordings = recordings.iterator();
	}

	@Override
	public Atom atom() {
		if (recordings.hasNext()) {
			Recording atom = recordings.next();
			return new RecordedSource(atom);
		}
		throw new CannotGenerateException("No more edge cases available");
	}

	@Override
	public List list() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Tree tree() {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
