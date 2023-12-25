package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.recording.*;

public final class RecordedSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	private final Iterator<Integer> iterator;
	private final Iterator<Recording> elements;
	private final Recording recording;
	private final GenSource backUpSource;

	public RecordedSource(Recording recording) {
		this(recording, null);
	}

	public RecordedSource(Recording recording, GenSource backUpSource) {
		this.recording = recording;
		this.backUpSource = backUpSource;
		if (recording instanceof ListRecording list)
			this.elements = list.elements().iterator();
		else
			this.elements = Collections.emptyIterator();
		if (recording instanceof AtomRecording atom)
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
		} else {
			if (backUpSource != null) {
				return backUpSource.atom().choose(maxExcluded);
			}
			throw new CannotGenerateException("No more choices!");
		}
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
				return new RecordedSource(elements.next(), backUpSource);
			}
			if (backUpSource != null) {
				return backUpSource.list().nextElement();
			}
			throw new CannotGenerateException("No more elements");
		}
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public GenSource head() {
		if (recording instanceof TreeRecording tree) {
			return new RecordedSource(tree.head(), backUpSource);
		}
		throw new CannotGenerateException("Source is not a tree");
	}

	@Override
	public GenSource child() {
		if (recording instanceof TreeRecording tree) {
			return new RecordedSource(tree.child(), backUpSource);
		}
		throw new CannotGenerateException("Source is not a tree");
	}
}