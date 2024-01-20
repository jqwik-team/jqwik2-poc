package jqwik2.internal.recording;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;

public abstract sealed class RecordedSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	protected Iterator<Integer> iterator;
	protected Iterator<Recording> elements;
	protected final Recording recording;
	protected final GenSource backUpSource;

	public static RecordedSource of(Recording recording) {
		return RecordedSource.of(recording, null);
	}

	public static RecordedSource of(Recording recording, GenSource backUpSource) {
		return switch (recording) {
			case ListRecording listRecording -> new RecordedList(recording, backUpSource);
			case TreeRecording treeRecording -> new RecordedTree(recording, backUpSource);
			case AtomRecording atomRecording -> new RecordedAtom(recording, backUpSource);
			case null, default -> throw new IllegalArgumentException("Unknown recording type: " + recording.getClass());
		};
	}

	private RecordedSource(Recording recording, GenSource backUpSource) {
		this.recording = recording;
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
		private RecordedList(Recording recording, GenSource backUpSource) {
			super(recording, backUpSource);
			if (recording instanceof ListRecording list)
				this.elements = list.elements().iterator();
			this.iterator = Collections.emptyIterator();
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
		private RecordedTree(Recording recording, GenSource backUpSource) {
			super(recording, backUpSource);
			this.elements = Collections.emptyIterator();
			this.iterator = Collections.emptyIterator();
		}

		@Override
		public Tree tree() {
			return this;
		}

		@Override
		public GenSource head() {
			if (recording instanceof TreeRecording tree) {
				return RecordedSource.of(tree.head(), backUpSource);
			}
			throw new CannotGenerateException("Source is not a tree");
		}

		@Override
		public GenSource child() {
			if (recording instanceof TreeRecording tree) {
				return RecordedSource.of(tree.child(), backUpSource);
			}
			throw new CannotGenerateException("Source is not a tree");
		}

	}

	static final class RecordedAtom extends RecordedSource {
		private RecordedAtom(Recording recording, GenSource backUpSource) {
			super(recording, backUpSource);
			this.elements = Collections.emptyIterator();
			if (recording instanceof AtomRecording atom)
				this.iterator = atom.choices().iterator();
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