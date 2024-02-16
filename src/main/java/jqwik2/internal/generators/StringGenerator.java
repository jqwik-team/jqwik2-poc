package jqwik2.internal.generators;

import java.util.*;

import jqwik2.api.*;

class StringGenerator extends AbstractCollectionGenerator<Integer, String> {
	StringGenerator(Generator<Integer> codepoints, int minLength, int maxLength) {
		super(codepoints, minLength, maxLength, Collections.emptySet());
	}

	@Override
	public String generate(GenSource source) {
		return generateCollection(source).stream()
										.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
										.toString();
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		return ExhaustiveSource.flatMap(
			ExhaustiveSource.choice(maxSize - minSize),
			head -> ExhaustiveSource.list(chooseSize(head), elementGenerator.exhaustive())
		);
	}

}
