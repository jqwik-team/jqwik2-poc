package jqwik2;

import java.util.*;

public final class RecordedSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
	private final Iterator<Integer> iterator;
	private final Iterator<SourceRecording> elements;
	private final SourceRecording recording;

	public RecordedSource(SourceRecording source) {
		this.recording = source;
		if (source instanceof ListRecording list)
			this.elements = list.elements().iterator();
		else
			this.elements = Collections.emptyIterator();
		this.iterator = source.iterator();
	}

	@Override
	public int choice(int max) {
		if (recording instanceof AtomRecording) {
			if (iterator.hasNext())
				return iterator.next();
			else
				throw new CannotGenerateException("No more choices!");
		}
		throw new CannotGenerateException("Source is not an atom");
	}

	@Override
	public GenSource nextElement() {
		if (elements.hasNext()) {
			return new RecordedSource(elements.next());
		}
		throw new CannotGenerateException("No more elements!");
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
	public <T extends GenSource> T nextElement(Class<T> sourceType) {
		if (recording instanceof ListRecording) {
			if (elements.hasNext()) {
				return (T) new RecordedSource(elements.next());
			}
			throw new CannotGenerateException("No more elements");
		}
		throw new CannotGenerateException("Source is not a list");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GenSource> T head(Class<T> sourceType) {
		if (recording instanceof TreeRecording tree) {
			return (T) new RecordedSource(tree.head());
		}
		throw new CannotGenerateException("Source is not a tree");
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends GenSource> T child(Class<T> sourceType) {
		if (recording instanceof TreeRecording tree) {
			return (T) new RecordedSource(tree.child());
		}
		throw new CannotGenerateException("Source is not a tree");
	}
}