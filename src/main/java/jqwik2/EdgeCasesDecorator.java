package jqwik2;

public class EdgeCasesDecorator<T> implements ValueGenerator<T> {

	private final ValueGenerator<T> baseGenerator;
	private final T[] edgeCases;

	private EdgeCasesDecorator(ValueGenerator<T> baseGenerator, T... edgeCases) {
		this.baseGenerator = baseGenerator;
		this.edgeCases = edgeCases;
	}

	@Override
	public T value(GenerationSource source) {
		source.next(1);
		return null;
	}
}
