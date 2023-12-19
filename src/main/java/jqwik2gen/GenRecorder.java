package jqwik2gen;

import java.util.*;

public class GenRecorder implements GenSource {

	private final GenSource source;
	private ConcreteRecorder recorder;

	public GenRecorder(GenSource source) {
		this.source = source;
	}

	public SourceRecording recording() {
		if (recorder == null) {
			throw new IllegalStateException("Recording has not been started");
		}
		return recorder.recording();
	}

	@Override
	public Atom atom() {
		recorder = new AtomRecorder(source.atom());
		return (Atom) recorder;
	}

	@Override
	public List list() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Tree tree() {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private class AtomRecorder extends ConcreteRecorder implements Atom {

		private final Atom source;
		private final java.util.List<Integer> seeds = new ArrayList<>();

		private AtomRecorder(Atom source) {
			this.source = source;
		}

		@Override
		SourceRecording recording() {
			return new AtomRecording(seeds);
		}

		@Override
		public Atom atom() {
			return source.atom();
		}

		@Override
		public int choice(int max) {
			int choice = source.choice(max);
			seeds.add(choice);
			return choice;
		}
	}

	private static abstract class ConcreteRecorder implements GenSource {

		abstract SourceRecording recording();

		@Override
		public Atom atom() {
			throw new UnsupportedOperationException("Should never be called");
		}

		@Override
		public List list() {
			throw new UnsupportedOperationException("Should never be called");
		}

		@Override
		public Tree tree() {
			throw new UnsupportedOperationException("Should never be called");
		}
	}
}
