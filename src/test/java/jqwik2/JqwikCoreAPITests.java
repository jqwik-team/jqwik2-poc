package jqwik2;

import java.time.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

@Group
class JqwikCoreAPITests {

	// @Example
	void apiIdeas() {
/*
		var property = Jqwik.property("my").forAll(Numbers.integers().between(0, 100))
							.classify(
								caseOf("Even number", 40.0, i -> i % 2 == 0),
								caseOf("Odd number", 40.0, i -> i % 2 != 0)
							)
							.check((i, runContext) -> {
								if (i % 2 == 0) {
									return i % 2 == 0;
								} else {
									return i % 2 != 0;
								}
							});

		property.validate();

		PropertyRunStrategy strategy = PropertyRunStrategy.builder().withMaxTries(1000);
		PropertyRunResult result = property.validate(strategy);

		PropertyRunResult result = property.validate(strategy, failureDatabase);

		PropertyRunResult result = property.validate(strategy, failureDatabase, onFailedHandler, onAbortHandler);

		property.validateAndThrow(strategy);

		property.validateAndThrow(strategy, failureDatabase);

		property.validateStatistically(strategy, 90.0, 3);

		property.analyze();

		property.analyze(failureDatabase);

*/
	}

	@Group
	class RunStrategyBuilder {

		PropertyRunStrategy.Builder builder = PropertyRunStrategy.builder();

		@Example
		void defaultStrategy() {
			PropertyRunStrategy strategy = builder.build();
			assertThat(strategy.maxTries()).isEqualTo(JqwikDefaults.defaultMaxTries());
		}

		@Example
		void changeStandardAttributes() {
			PropertyRunStrategy strategy =
				builder.withMaxTries(42)
					   .withMaxRuntime(Duration.ofSeconds(42))
					   .withFilterOutDuplicateSamples(true)
					   .withGeneration(PropertyRunStrategy.GenerationMode.EXHAUSTIVE)
					   .withAfterFailure(PropertyRunStrategy.AfterFailureMode.SAMPLES_ONLY)
					   .withConcurrency(PropertyRunStrategy.ConcurrencyMode.VIRTUAL_THREADS)
					   .build();

			assertThat(strategy.maxTries()).isEqualTo(42);
			assertThat(strategy.maxRuntime()).isEqualTo(Duration.ofSeconds(42));
			assertThat(strategy.filterOutDuplicateSamples()).isTrue();
			assertThat(strategy.generation()).isEqualTo(PropertyRunStrategy.GenerationMode.EXHAUSTIVE);
			assertThat(strategy.afterFailure()).isEqualTo(PropertyRunStrategy.AfterFailureMode.SAMPLES_ONLY);
			assertThat(strategy.concurrency()).isEqualTo(PropertyRunStrategy.ConcurrencyMode.VIRTUAL_THREADS);
		}

		@Example
		void changeRandomizedGenerationAttributes() {
			PropertyRunStrategy strategy =
				builder.withGeneration(PropertyRunStrategy.GenerationMode.RANDOMIZED)
					   .withSeedSupplier(() -> "42")
					   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.MIXIN)
					   .withShrinking(PropertyRunStrategy.ShrinkingMode.FULL)
					   .build();

			assertThat(strategy.generation()).isEqualTo(PropertyRunStrategy.GenerationMode.RANDOMIZED);
			assertThat(strategy.seedSupplier().get()).isEqualTo("42");
			assertThat(strategy.edgeCases()).isEqualTo(PropertyRunStrategy.EdgeCasesMode.MIXIN);
			assertThat(strategy.shrinking()).isEqualTo(PropertyRunStrategy.ShrinkingMode.FULL);
		}

		@Example
		void changeSamplesGenerationAttributes() {
			var sample1 = new SampleRecording(Recording.choice(42));
			var sample2 = new SampleRecording(Recording.choice(43));
			PropertyRunStrategy strategy =
				builder.withGeneration(PropertyRunStrategy.GenerationMode.SAMPLES)
					   .withSamples(List.of(sample1, sample2))
					   .withShrinking(PropertyRunStrategy.ShrinkingMode.OFF)
					   .build();

			assertThat(strategy.generation()).isEqualTo(PropertyRunStrategy.GenerationMode.SAMPLES);
			assertThat(strategy.samples()).containsExactly(sample1, sample2);
			assertThat(strategy.shrinking()).isEqualTo(PropertyRunStrategy.ShrinkingMode.OFF);
		}
	}

}
