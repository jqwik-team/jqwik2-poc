package jqwik2;

import java.util.List;
import java.util.Objects;

public final class GeneratedValue<T> {
	private final T value;
	private final ValueGenerator<T> generator;
	private final List<Integer> seeds;

	public GeneratedValue(T value, ValueGenerator<T> generator, List<Integer> seeds) {
		this.value = value;
		this.generator = generator;
		this.seeds = seeds;
	}

	public T value() {
		return value;
	}

	public List<Integer> seeds() {
		return seeds;
	}

	public GeneratedValue<T> regenerate() {
		FixedGenerationSource fixedSource = new FixedGenerationSource(seeds());
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
		return Objects.hash(value, seeds);
	}

	@Override
	public String toString() {
		return "(%s, %s)".formatted(value, seeds);
	}

}
