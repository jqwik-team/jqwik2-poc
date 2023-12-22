package jqwik2;

import java.util.*;

import jqwik2.recording.*;

public class IntegerGenerator implements Generator<Integer> {
	private final int min;
	private final int max;

	public IntegerGenerator(int min, int max) {
		this.min = min;
		this.max = max;
	}

	@Override
	public Integer generate(GenSource source) {
		return source.chooseInt(min, max);
	}

	@Override
	public GenSource edgeCases() {
		return new RecordedEdgeCases(edgeCaseRecordings());
	}

	private List<Recording> edgeCaseRecordings() {
		return List.of(
			Recording.atom(min, 1),
			Recording.atom(1, 1),
			Recording.atom(0, 0),
			Recording.atom(1, 0),
			Recording.atom(max, 0)
		);
	}
}
