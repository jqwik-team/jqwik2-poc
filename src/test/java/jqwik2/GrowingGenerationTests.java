package jqwik2;

import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.growing.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class GrowingGenerationTests {

	@Example
	void smallIntegers() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 100)
		);

		AtomicInteger counter = new AtomicInteger(0);
		for (SampleSource sampleSource : new IterableGrowingSource()) {
			sampleGenerator.generate(sampleSource).ifPresent(sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			});
		}
		assertThat(counter.get()).isEqualTo(101);
	}

	@Example
	void positiveAndNegativeIntegers() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(-5, 10)
		);

		// There'll be duplicate values since different sources can lead to same value
		Set<List<Object>> values = new HashSet<>();
		for (SampleSource sampleSource : new IterableGrowingSource()) {
			sampleGenerator.generate(sampleSource).ifPresent(sample -> {
				// System.out.println(sample);
				values.add(sample.values());
			});
		}
		assertThat(values).hasSize(16);
	}

	@Example
	void sampleWithThreeParameters() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 2),
			BaseGenerators.integers(0, 3),
			BaseGenerators.integers(0, 4)
		);

		AtomicInteger counter = new AtomicInteger(0);
		for (SampleSource sampleSource : new IterableGrowingSource()) {
			sampleGenerator.generate(sampleSource).ifPresent(sample -> {
				counter.incrementAndGet();
				System.out.println(sample);
			});
		}
		assertThat(counter.get()).isEqualTo(60);
	}

}
