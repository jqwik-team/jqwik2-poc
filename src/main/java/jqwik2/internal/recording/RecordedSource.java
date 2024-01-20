package jqwik2.internal.recording;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;

public abstract sealed class RecordedSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	protected final GenSource backUpSource;

	// TODO: Return concrete type of GenSource instead of generic RecordedSource
	public static RecordedSource of(Recording recording) {
		return RecordedSource.of(recording, null);
	}

	public static RecordedSource of(Recording recording, GenSource backUpSource) {
		return switch (recording) {
			case ListRecording listRecording -> new RecordedList(listRecording, backUpSource);
			case TreeRecording treeRecording -> new RecordedTree(treeRecording, backUpSource);
			case AtomRecording atomRecording -> new RecordedAtom(atomRecording, backUpSource);
			case null, default -> throw new IllegalArgumentException("Unknown recording type: " + recording.getClass());
		};
	}

	private RecordedSource(GenSource backUpSource) {
		this.backUpSource = backUpSource;
	}

	@Override
	public int choose(int maxExcluded) {
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public Atom atom() {
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public List list() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public Tree tree() {
		throw new CannotGenerateException("Source is not a tree");
	}

	@Override
	public GenSource nextElement() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public GenSource head() {
		throw new CannotGenerateException("Source is not a tree");
	}

	@Override
	public GenSource child() {
		throw new CannotGenerateException("Source is not a tree");
	}

	static final class RecordedList extends RecordedSource {

		private final  Iterator<Recording> elements;

		private RecordedList(ListRecording recording, GenSource backUpSource) {
			super(backUpSource);
			this.elements = recording.elements().iterator();
		}

		@Override
		public List list() {
			return this;
		}

		@Override
		public GenSource nextElement() {
			if (elements.hasNext()) {
				return RecordedSource.of(elements.next(), backUpSource);
			}
			if (backUpSource != null) {
				return backUpSource.list().nextElement();
			}
			throw new CannotGenerateException("No more elements");
		}

	}

	static final class RecordedTree extends RecordedSource {

		private final TreeRecording recording;

		private RecordedTree(TreeRecording recording, GenSource backUpSource) {
			super(backUpSource);
			this.recording = recording;
		}

		@Override
		public Tree tree() {
			return this;
		}

		@Override
		public GenSource head() {
			return RecordedSource.of(recording.head(), backUpSource);
		}

		@Override
		public GenSource child() {
			return RecordedSource.of(recording.child(), backUpSource);
		}

	}

	static final class RecordedAtom extends RecordedSource {
		private final Iterator<Integer> iterator;

		private RecordedAtom(AtomRecording recording, GenSource backUpSource) {
			super(backUpSource);
			this.iterator = recording.choices().iterator();
		}

		@Override
		public Atom atom() {
			return this;
		}

		@Override
		public int choose(int maxExcluded) {
			if (iterator.hasNext()) {
				if (maxExcluded == 0) {
					return 0;
				}
				return iterator.next() % maxExcluded;
			} else {
				if (backUpSource != null) {
					return backUpSource.atom().choose(maxExcluded);
				}
				throw new CannotGenerateException("No more choices!");
			}
		}

		@Override
		public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
			return choose(maxExcluded);
		}
	}
}