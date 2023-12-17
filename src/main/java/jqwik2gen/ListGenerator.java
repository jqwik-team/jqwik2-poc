package jqwik2gen;

import java.util.*;

public class ListGenerator<T> implements Generator<List<T>> {
	private final Generator<T> elements;
	private final int maxSize;

	public ListGenerator(Generator<T> elements, int maxSize) {
		this.elements = elements;
		this.maxSize = maxSize;
	}

	@Override
	public Shrinkable<List<T>> generate(GenSource source) {
		AtomicRecording sizeRecording = new AtomicRecording();
		OldTreeRecording recorded = new OldTreeRecording(
			sizeRecording,
			new ArrayList<>()
		);

		int size = sizeRecording.push(source.next(maxSize + 1));
		List<Shrinkable<T>> shrinkables = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			GenSource childSource = source.child();
			Shrinkable<T> shrinkableElement = elements.generate(childSource);
			shrinkables.add(shrinkableElement);
			recorded.pushChild(shrinkableElement.recording());
		}
		return new GeneratedShrinkable<>(
			shrinkables.stream().map(Shrinkable::value).toList(),
			this,
			recorded
		);
	}
}
