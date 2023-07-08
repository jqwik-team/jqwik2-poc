package jqwik2;

import java.util.ArrayList;
import java.util.List;

public class EdgeCasesDecorator<T> implements ValueGenerator<T> {

	private final ValueGenerator<T> baseGenerator;
	private final T[] edgeCases;

	@SafeVarargs
	public EdgeCasesDecorator(ValueGenerator<T> baseGenerator, T... edgeCases) {
		this.baseGenerator = baseGenerator;
		this.edgeCases = edgeCases;
	}

	@Override
	public GeneratedValue<T> generate(GenerationSource source) {
		int useEdgeCasesSeed = source.next(1, 0, 1)[0];
		List<Integer> seeds = new ArrayList<>();
		seeds.add(useEdgeCasesSeed);

		boolean useEdgeCases = useEdgeCasesSeed == 1;
		GeneratedValue<T> generated = null;
		if (useEdgeCases) {
			generated = edgeCase(source);
		} else {
			generated = baseGenerator.generate(source);
		}
		return new GeneratedValue<>(generated.value(), this, seeds, List.of(generated));
	}

	private GeneratedValue<T> edgeCase(GenerationSource source) {
		int selectedIndex = source.next(1, 0, edgeCases.length - 1)[0];
		return new GeneratedValue<>(edgeCases[selectedIndex], this, List.of(selectedIndex));
	}
}
