package jqwik2gen;

import java.util.*;

public final class RecordedSource implements GenSource {
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
	public int next(int max) {
		if (iterator.hasNext())
			return iterator.next();
		else
			throw new CannotGenerateException("No more values!");
	}

	@Override
	public GenSource child() {
		if (recording instanceof TreeRecording tree) {
			return new RecordedSource(tree.child());
		}
		throw new CannotGenerateException("No child!");
	}

	@Override
	public GenSource next() {
		if (elements.hasNext()) {
			return new RecordedSource(elements.next());
		}
		throw new CannotGenerateException("No more elements!");
	}
}