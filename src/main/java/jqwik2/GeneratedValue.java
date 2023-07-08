package jqwik2;

import java.util.List;
import java.util.Objects;
import java.util.stream.*;

public final class GeneratedValue<T> {
	private final T value;
	private final ValueGenerator<T> generator;
	private final List<Integer> baseSeeds;
	private final List<GeneratedValue<?>> children;

	public GeneratedValue(T value, ValueGenerator<T> generator, List<Integer> seeds) {
		this(value, generator, seeds, List.of());
	}

	public GeneratedValue(T value, ValueGenerator<T> generator, List<Integer> baseSeeds, List<GeneratedValue<?>> children) {
		this.value = value;
		this.generator = generator;
		this.baseSeeds = baseSeeds;
		this.children = children;
	}

	public T value() {
		return value;
	}

	public Stream<Integer> seeds() {
		return Stream.concat(baseSeeds.stream(), children.stream().flatMap(GeneratedValue::seeds));
	}

	public GeneratedValue<T> regenerate() {
		FixedGenerationSource fixedSource = new FixedGenerationSource(seeds().toList());
		return generator.generate(fixedSource);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (GeneratedValue<?>) obj;
		return Objects.equals(this.value, that.value) &&
				   Objects.equals(this.generator, that.generator);
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, baseSeeds, children);
	}

	@Override
	public String toString() {
		return "(%s, %s)".formatted(value, baseSeeds);
	}

}
