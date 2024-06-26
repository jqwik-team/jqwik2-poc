package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.stateful.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.growing.*;

import net.jqwik.api.*;

import static jqwik2.api.arbitraries.Values.*;
import static jqwik2.api.validation.PropertyValidationStatus.*;
import static org.assertj.core.api.Assertions.*;

class GrowingGenerationTests {

	@Example
	void smallIntegers() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 100)
		);

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(101);
	}

	@Example
	void positiveAndNegativeIntegers() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(-5, 10)
		);

		// There'll be duplicate values since different sources can lead to same value
		Set<List<Object>> values = new HashSet<>();
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				// System.out.println(sample);
				values.add(sample.values());
			}
		);
		assertThat(values).hasSize(16);
	}

	@Example
	void growSamplesInPropertyCase() {
		List<Generator<?>> generators = List.of(
			BaseGenerators.integers(Integer.MIN_VALUE, Integer.MAX_VALUE),
			BaseGenerators.choose(List.of("a", "b", "c"))
		);

		AtomicInteger counter = new AtomicInteger(0);
		Tryable tryable = Tryable.from(args -> {
			counter.incrementAndGet();
			// System.out.println(args);
			return counter.get() < 100;
		});
		var runner = new PropertyRunner(generators, tryable);

		var result = runner.run(PropertyRunConfiguration.growing(
			10000, false, Duration.ofSeconds(10)
		));

		assertThat(result.status()).isEqualTo(FAILED);
		assertThat(counter.get()).isEqualTo(100);
	}

	@Example
	void sampleWithTwoParameters() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			Numbers.integers().between(-100, 100).generator(),
			Numbers.integers().between(10, 20).generator()
		);

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			},
			100
		);
		assertThat(counter.get()).isEqualTo(100);
	}

	@Example
	void stopOnMaximumDepth() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			Numbers.integers().between(0, 100).generator(),
			Numbers.integers().between(0, 4).generator()
		);

		final int maxDepth = 7;
		AtomicInteger counter = new AtomicInteger(0);
		GuidedGeneration iterator = (GuidedGeneration) new IterableGrowingSource(maxDepth).iterator();
		while (iterator.hasNext()) {
			SampleSource sampleSource = iterator.next();
			sampleGenerator.generate(sampleSource).ifPresentOrElse(
				sample -> {
					counter.incrementAndGet();
					// System.out.println(sample);
					iterator.guide(null, null);
				},
				() -> iterator.onEmptyGeneration(sampleSource)
			);
		}
		assertThat(counter.get()).isEqualTo(30);
	}

	@Example
	void sampleWithThreeParameters() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 2),
			BaseGenerators.integers(0, 3),
			BaseGenerators.integers(0, 4)
		);

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(60);
	}

	@Example
	void mappedValues() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 10).map(i -> Integer.toString(i))
		);

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(11);
	}

	@Example
	void filteredValues() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 100).filter(i -> i % 3 == 0)
		);

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(34);
	}

	@Example
	void flatMappedValues() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 3).flatMap(i -> BaseGenerators.integers(0, i).map(j -> List.of(i, j)))
		);

		Set<List<Object>> values = new HashSet<>();
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				values.add(sample.values());
				// System.out.println(sample);
			}
		);
		assertThat(values).containsExactlyInAnyOrder(
			List.of(List.of(0, 0)),
			List.of(List.of(1, 0)),
			List.of(List.of(1, 1)),
			List.of(List.of(2, 0)),
			List.of(List.of(2, 1)),
			List.of(List.of(2, 2)),
			List.of(List.of(3, 0)),
			List.of(List.of(3, 1)),
			List.of(List.of(3, 2)),
			List.of(List.of(3, 3))
		);
	}

	@Example
	void listOfIntegers() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 5).list(0, 2)
		);
		sampleGenerator.filterOutDuplicates();

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(43);
	}

	@Example
	void setOfIntegers() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.integers(0, 5).set(0, 2)
		);
		sampleGenerator.filterOutDuplicates();

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(37);
	}

	@Example
	void oneOfGenerator() {
		SampleGenerator sampleGenerator = SampleGenerator.from(
			BaseGenerators.oneOf(List.of(
				BaseGenerators.just(List.of(0, 0, 0)),
				BaseGenerators.integers(0, 5).list(0, 2),
				BaseGenerators.choose(List.of(
					List.of(1, 2, 3),
					List.of(4, 5, 6)
				))
			))
		);
		sampleGenerator.filterOutDuplicates();

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(46); // 1 + 43 + 2
	}

	@Example
	void lazyRecursiveGenerator() {
		Generator<Integer> combined = recursiveInts().generator();

		SampleGenerator sampleGenerator = SampleGenerator.from(combined);

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}, 100 // without limit this would run forever
		);
		assertThat(counter.get()).isEqualTo(100);
	}

	private static Arbitrary<Integer> recursiveInts() {
		return Values.lazy(
			() -> Values.oneOf(
				just(1),
				recursiveInts().map(i -> i + 1)
			));
	}

	@Example
	void simpleCombinations() {
		Arbitrary<Integer> ints = Numbers.integers().between(1, 5);
		Arbitrary<Integer> tens = Numbers.integers().between(1, 5).map(i -> i * 10);

		Generator<Integer> combined = BaseGenerators.combine(sampler -> {
			int anInt = sampler.draw(ints);
			int aTen = sampler.draw(tens);
			return anInt + aTen;
		});

		SampleGenerator sampleGenerator = SampleGenerator.from(combined);

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(25);
	}

	@Example
	void combinationWithFlatMapping() {
		var sizes = Numbers.integers().between(1, 3);
		Generator<List<Integer>> combined = BaseGenerators.combine(sampler -> {
			int size = sampler.draw(sizes);
			return sampler.draw(Numbers.integers().between(-1, 1).list().ofSize(size));
		});

		SampleGenerator sampleGenerator = SampleGenerator.from(combined);
		sampleGenerator.filterOutDuplicates();

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				// System.out.println(sample);
			}
		);
		assertThat(counter.get()).isEqualTo(39);
	}

	@Example
	void statefulChain() {

		ChainArbitrary<Integer> chains =
			Chain.startWith(() -> 1)
				 .withTransformation(i -> Numbers.integers().between(1, i).map(
					 j -> Transformer.transform("+" + j, n -> n + j)
				 ))
				 .withMaxTransformations(5);

		SampleGenerator sampleGenerator = SampleGenerator.from(chains.generator());

		AtomicInteger counter = new AtomicInteger(0);
		forAllGrowingSamples(
			sampleGenerator,
			sample -> {
				counter.incrementAndGet();
				Chain<Integer> chain = (Chain<Integer>) sample.values().get(0);
				chain.forEachRemaining(i -> {});
				// System.out.println(chain.transformations());
			}
		);
		// There can be duplicates since different sources can lead to same value
		// due to automatic expansion of lists and tuples
		assertThat(counter.get()).isGreaterThanOrEqualTo(397);
	}

	// IterableGrowingSource cannot be directly iterated since it is a SequentialGuidedSource that requires guidance being triggered
	private static void forAllGrowingSamples(SampleGenerator sampleGenerator, Consumer<Sample> whenGenerated) {
		forAllGrowingSamples(sampleGenerator, whenGenerated, 1000);
	}

	private static void forAllGrowingSamples(SampleGenerator sampleGenerator, Consumer<Sample> whenGenerated, int max) {
		AtomicInteger counter = new AtomicInteger(0);
		GuidedGeneration iterator = (GuidedGeneration) new IterableGrowingSource().iterator();
		while (iterator.hasNext() && counter.get() < max) {
			SampleSource sampleSource = iterator.next();
			try {
				sampleGenerator.generate(sampleSource).ifPresentOrElse(
					sample -> {
						whenGenerated.accept(sample);
						iterator.guide(null, null);
						counter.incrementAndGet();
					},
					() -> iterator.onEmptyGeneration(sampleSource)
				);
			} catch (CannotGenerateException cge) {
				break;
			}
		}
	}

}
