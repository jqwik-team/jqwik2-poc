package jqwik2;

import java.util.*;

import jqwik2.api.*;

public class ListGenerator<T> implements Generator<List<T>> {
	private final Generator<T> elementGenerator;
	private final int maxSize;

	public ListGenerator(Generator<T> elementGenerator, int maxSize) {
		this.elementGenerator = elementGenerator;
		this.maxSize = maxSize;
	}

	@Override
	public List<T> generate(GenSource source) {
		GenSource.Tree listSource = source.tree();
		GenSource.Atom sizeSource = listSource.head().atom();

		int size = sizeSource.choose(maxSize + 1);

		List<T> elements = new ArrayList<>(size);
		GenSource.List elementsSource = listSource.child().list();
		for (int i = 0; i < size; i++) {
			GenSource elementSource = elementsSource.nextElement();
			T element = elementGenerator.generate(elementSource);
			elements.add(element);
		}
		return elements;
	}

}
