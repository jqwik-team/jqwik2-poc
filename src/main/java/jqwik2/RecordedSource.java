package jqwik2;

import java.util.*;

import jqwik2.recording.*;

public final class RecordedSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	private final Iterator<Integer> iterator;
	private final Iterator<Recording> elements;
	private final Recording recording;

	public RecordedSource(Recording source) {
		this.recording = source;
		if (source instanceof ListRecording list)
			this.elements = list.elements().iterator();
		else
			this.elements = Collections.emptyIterator();
		if (source instanceof AtomRecording atom)
			this.iterator = atom.choices().iterator();
		else
			this.iterator = Collections.emptyIterator();
	}

	@Override
	public int choose(int maxExcluded) {
		if (iterator.hasNext()) {
			if (maxExcluded == 0) {
				return 0;
			}
			return iterator.next() % maxExcluded;
		} else
			throw new CannotGenerateException("No more choices!");
	}

	@Override
	public Atom atom() {
		if (!(recording instanceof AtomRecording)) {
			throw new CannotGenerateException("Source is not an atom");
		}
		return this;
	}

	@Override
	public List list() {
		if (!(recording instanceof ListRecording)) {
			throw new CannotGenerateException("Source is not a list");
		}
		return this;
	}

	@Override
	public Tree tree() {
		if (!(recording instanceof TreeRecording)) {
			throw new CannotGenerateException("Source is not a tree");
		}
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public GenSource nextElement() {
		if (recording instanceof ListRecording) {
			if (elements.hasNext()) {
				return new RecordedSource(elements.next());
			}
			throw new CannotGenerateException("No more elements");
		}
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public GenSource head() {
		if (recording instanceof TreeRecording tree) {
			return new RecordedSource(tree.head());
		}
		throw new CannotGenerateException("Source is not a tree");
	}

	@Override
	public GenSource child() {
		if (recording instanceof TreeRecording tree) {
			return new RecordedSource(tree.child());
		}
		throw new CannotGenerateException("Source is not a tree");
	}
}