package jqwik2gen;

import java.util.*;

public final class RecordedSource implements GenSource {
	private final Iterator<Integer> iterator;
	private final Iterator<SourceRecording> children;
	private final SourceRecording recording;

	public RecordedSource(SourceRecording source) {
		this.recording = source;
		this.children = source.children().iterator();
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
		if (children.hasNext())
			return new RecordedSource(children.next());
		else
			throw new CannotGenerateException("No more children!");
	}
}
