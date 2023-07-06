package jqwik2;

public class EdgeCasesDecorator<T> implements ValueGenerator<T> {

	private final ValueGenerator<T> baseGenerator;
	private final T[] edgeCases;

	@SafeVarargs
	public EdgeCasesDecorator(ValueGenerator<T> baseGenerator, T... edgeCases) {
		this.baseGenerator = baseGenerator;
		this.edgeCases = edgeCases;
	}

	@Override
	public T value(GenerationSource source) {
		boolean useEdgeCases = source.next(1, 0, 1)[0] == 1;
		if (useEdgeCases) {
			return edgeCase(source);
		} else {
			return baseGenerator.value(source);
		}
	}

	private T edgeCase(GenerationSource source) {
		int selectedIndex = source.next(1, 0, edgeCases.length - 1)[0];
		return edgeCases[selectedIndex];
	}
}