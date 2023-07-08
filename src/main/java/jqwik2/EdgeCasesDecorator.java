package jqwik2;

import java.util.ArrayList;
import java.util.List;

public class EdgeCasesDecorator<T> implements ValueGenerator<T> {

	private final ValueGenerator<T> baseGenerator;
	private final List<T> edgeCases;

	public EdgeCasesDecorator(ValueGenerator<T> baseGenerator, List<T> edgeCases) {
		this.baseGenerator = baseGenerator;
		if (edgeCases.isEmpty()) {
			throw new IllegalArgumentException("Edge cases must not be empty");
		}
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
		int selectedIndex = source.next(1, 0, edgeCases.size() - 1)[0];
		return new GeneratedValue<>(edgeCases.get(selectedIndex), this, List.of(selectedIndex));
	}
}
