package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class RecordingBasedExhaustiveGenerator implements ExhaustiveGenerator {

	private final Iterable<Recording> recordings;
	private final Long maxCount;

	public RecordingBasedExhaustiveGenerator(Iterable<Recording> recordings, Long maxCount) {
		this.recordings = recordings;
		this.maxCount = maxCount;
	}

	@Override
	public Optional<Long> maxCount() {
		return Optional.ofNullable(maxCount);
	}

	@Override
	public Iterator<Recording> iterator() {
		return recordings.iterator();
	}
}
