package jqwik2gen;

import java.util.*;

public final class RecordedSource implements GenSource {
	private final Iterator<Integer> iterator;
	private final Iterator<SourceRecording> children;
	private final SourceRecording recording;

	public RecordedSource(SourceRecording source) {
		this.recording = source;
		if (source instanceof ListRecording list)
			this.children = list.elements().iterator();
		else
			this.children = Collections.emptyIterator();
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
		if (recording instanceof ListRecording && children.hasNext()) {
			return new RecordedSource(children.next());
		}
		if (recording instanceof TreeRecording tree) {
			return new RecordedSource(tree.child());
		}
		throw new CannotGenerateException("No more children!");
	}

}