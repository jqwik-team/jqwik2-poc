package jqwik2.internal.recording;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;

public abstract sealed class RecordedSource<T extends Recording> implements GenSource {
	protected final GenSource backUpSource;
	protected final T recording;

	public static GenSource of(Recording recording) {
		return RecordedSource.of(recording, null);
	}

	public static GenSource of(Recording recording, GenSource backUpSource) {
		return switch (recording) {
			case ListRecording listRecording -> new RecordedList(listRecording, backUpSource);
			case TreeRecording treeRecording -> new RecordedTree(treeRecording, backUpSource);
			case TupleRecording treeRecording -> null;// new RecordedTuple(tupleRecording, backUpSource);
			case AtomRecording atomRecording -> new RecordedAtom(atomRecording, backUpSource);
			case null -> throw new IllegalArgumentException("Recording must not be null");
		};
	}

	private RecordedSource(T recording, GenSource backUpSource) {
		this.backUpSource = backUpSource;
		this.recording = recording;
	}

	@Override
	public Atom atom() {
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public Tuple tuple(int size) {
		throw new CannotGenerateException("Source is not a tuple");
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
	public String toString() {
		return "RecordedSource{%s}".formatted(recording.toString());
	}

	private static final class RecordedList extends RecordedSource<ListRecording> implements GenSource.List {

		private final Iterator<Recording> elements;

		private RecordedList(ListRecording recording, GenSource backUpSource) {
			super(recording, backUpSource);
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

	private static final class RecordedTree extends RecordedSource<TreeRecording> implements GenSource.Tree {

		private RecordedTree(TreeRecording recording, GenSource backUpSource) {
			super(recording, backUpSource);
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

	private static final class RecordedAtom extends RecordedSource<AtomRecording> implements GenSource.Atom {
		private final Iterator<Integer> iterator;

		private RecordedAtom(AtomRecording recording, GenSource backUpSource) {
			super(recording, backUpSource);
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