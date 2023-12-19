package jqwik2;

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
		GenSource.Tree listSource = source.tree();
		GenSource.Atom sizeSource = listSource.head(GenSource.Atom.class);

		int size = sizeSource.choice(maxSize + 1);
		AtomRecording sizeRecording = new AtomRecording(size);
		List<SourceRecording> elementRecordings = new ArrayList<>();

		List<Shrinkable<T>> shrinkables = new ArrayList<>(size);
		GenSource.List elementsSource = listSource.child(GenSource.List.class);
		for (int i = 0; i < size; i++) {
			GenSource elementSource = elementsSource.nextElement();
			Shrinkable<T> shrinkableElement = elements.generate(elementSource);
			shrinkables.add(shrinkableElement);
			elementRecordings.add(shrinkableElement.recording());
		}
		ListRecording listRecording = new ListRecording(elementRecordings);
		TreeRecording recorded = new TreeRecording(
			sizeRecording,
			listRecording
		);
		return new GeneratedShrinkable<>(
			shrinkables.stream().map(Shrinkable::value).toList(),
			this,
			recorded
		);
	}
}
