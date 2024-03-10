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

		property.validate(strategy).throwOnFailure();

		property.validate(strategy, failureDatabase);

		property.validateStatistically(strategy, 90.0, 3);

		property.analyze();

		property.analyze(failureDatabase);

*/
	}

	@Group
	class RunStrategyBuilder {

		PropertyValidationStrategy.Builder builder = PropertyValidationStrategy.builder();

		@Example
		void defaultStrategy() {
			PropertyValidationStrategy strategy = builder.build();
			assertThat(strategy.maxTries()).isEqualTo(JqwikDefaults.defaultMaxTries());
		}

		@Example
		void changeStandardAttributes() {
			PropertyValidationStrategy strategy =
				builder.withMaxTries(42)
					   .withMaxRuntime(Duration.ofSeconds(42))
					   .withFilterOutDuplicateSamples(true)
					   .withGeneration(PropertyValidationStrategy.GenerationMode.EXHAUSTIVE)
					   .withAfterFailure(PropertyValidationStrategy.AfterFailureMode.SAMPLES_ONLY)
					   .withConcurrency(PropertyValidationStrategy.ConcurrencyMode.VIRTUAL_THREADS)
					   .build();

			assertThat(strategy.maxTries()).isEqualTo(42);
			assertThat(strategy.maxRuntime()).isEqualTo(Duration.ofSeconds(42));
			assertThat(strategy.filterOutDuplicateSamples()).isTrue();
			assertThat(strategy.generation()).isEqualTo(PropertyValidationStrategy.GenerationMode.EXHAUSTIVE);
			assertThat(strategy.afterFailure()).isEqualTo(PropertyValidationStrategy.AfterFailureMode.SAMPLES_ONLY);
			assertThat(strategy.concurrency()).isEqualTo(PropertyValidationStrategy.ConcurrencyMode.VIRTUAL_THREADS);
		}

		@Example
		void changeRandomizedGenerationAttributes() {
			PropertyValidationStrategy strategy =
				builder.withGeneration(PropertyValidationStrategy.GenerationMode.RANDOMIZED)
					   .withSeedSupplier(() -> "42")
					   .withEdgeCases(PropertyValidationStrategy.EdgeCasesMode.MIXIN)
					   .withShrinking(PropertyValidationStrategy.ShrinkingMode.FULL)
					   .build();

			assertThat(strategy.generation()).isEqualTo(PropertyValidationStrategy.GenerationMode.RANDOMIZED);
			assertThat(strategy.seedSupplier().get()).isEqualTo("42");
			assertThat(strategy.edgeCases()).isEqualTo(PropertyValidationStrategy.EdgeCasesMode.MIXIN);
			assertThat(strategy.shrinking()).isEqualTo(PropertyValidationStrategy.ShrinkingMode.FULL);
		}

		@Example
		void changeSamplesGenerationAttributes() {
			var sample1 = new SampleRecording(Recording.choice(42));
			var sample2 = new SampleRecording(Recording.choice(43));
			PropertyValidationStrategy strategy =
				builder.withGeneration(PropertyValidationStrategy.GenerationMode.SAMPLES)
					   .withSamples(List.of(sample1, sample2))
					   .withShrinking(PropertyValidationStrategy.ShrinkingMode.OFF)
					   .build();

			assertThat(strategy.generation()).isEqualTo(PropertyValidationStrategy.GenerationMode.SAMPLES);
			assertThat(strategy.samples()).containsExactly(sample1, sample2);
			assertThat(strategy.shrinking()).isEqualTo(PropertyValidationStrategy.ShrinkingMode.OFF);
		}
	}

}
