package jqwik2gen;

import java.util.*;

public class GenRecorder implements GenSource {

	private final GenSource source;
	private SourceRecording recording;

	public GenRecorder(GenSource source) {
		this.source = source;
	}

	public SourceRecording recording() {
		return recording;
	}

	@Override
	public Atom atom() {
		return new AtomRecorder(source.atom());
	}

	@Override
	public List list() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Tree tree() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private class AtomRecorder implements Atom {
		private final Atom atom;

		private AtomRecorder(Atom atom) {
			this.atom = atom;
			recording = new AtomRecording();
		}

		@Override
		public Atom atom() {
			return atom.atom();
		}

		@Override
		public List list() {
			throw new UnsupportedOperationException("Not yet implemented");
		}

		@Override
		public Tree tree() {
			throw new UnsupportedOperationException("Not yet implemented");
		}

		@Override
		public int choice(int max) {
			int choice = atom.choice(max);
			((AtomRecording) recording).pushChoice(choice);
			return choice;
		}
	}
}
