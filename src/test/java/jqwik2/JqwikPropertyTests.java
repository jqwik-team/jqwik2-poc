package jqwik2;

import java.time.*;
import java.util.*;
import java.util.function.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.Assume;
import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;
import jqwik2.internal.*;
import org.opentest4j.*;

import net.jqwik.api.*;

import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JqwikPropertyTests {

	@Example
	void propertyWith1ParameterSucceeds() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(Numbers.integers()).check(i -> {
			assertThat(i).isInstanceOf(Integer.class);
			return true;
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);

		result = property.forAll(Numbers.integers()).verify(i -> {
			// Thread.sleep(10);
			// System.out.println(i);
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	@Example
	void propertyWith1ParameterFails() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(Numbers.integers()).check(i -> false);
		assertThat(result.isFailed()).isTrue();
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);

		result = property.forAll(Numbers.integers()).verify(i -> {
			// Thread.sleep(10);
			// System.out.println(i);
			throw new AssertionError("failed");
		});
		assertThat(result.isFailed()).isTrue();
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);
	}

	@Example
	void failingPropertyThrowsExceptionWhenFailed() {
		var property = new JqwikProperty();
		property.onFailed((result, throwable) -> {
			ExceptionSupport.throwAsUnchecked(throwable);
		});

		assertThatThrownBy(
			() -> property.forAll(Values.just(42)).check(i -> false)
		).isInstanceOf(AssertionError.class)
		 .hasMessageContaining("failed");
	}

	@Example
	void abortedPropertyThrowsAbortionErrorWhenAborted() {
		var property = new JqwikProperty();
		property.onAbort(abortionReason -> {
			abortionReason.ifPresent(ExceptionSupport::throwAsUnchecked);
			throw new TestAbortedException("Property aborted for unknown reason");
		});

		var throwingArbitrary = new Arbitrary<Integer>() {
			@Override
			public Generator<Integer> generator() {
				throw new TestAbortedException("Property aborted because of thrown exception");
			}
		};
		assertThatThrownBy(
			() -> property.forAll(throwingArbitrary).verify(i -> {})
		).isInstanceOf(TestAbortedException.class);
	}

	@Example
	void onFailureNotification() {
		var property = new JqwikProperty();

		BiConsumer<PropertyRunResult, Throwable> consumer1 = mock(BiConsumer.class);
		BiConsumer<PropertyRunResult, Throwable> consumer2 = mock(BiConsumer.class);
		property.onFailed(consumer1);
		property.onFailed(consumer2);

		PropertyRunResult result = property.forAll(Values.just(10)).check(i -> false);
		assertThat(result.isFailed()).isTrue();

		verify(consumer1).accept(any(PropertyRunResult.class), any(AssertionError.class));
		verify(consumer2).accept(any(PropertyRunResult.class), any(AssertionError.class));
	}

	@Example
	void propertyWith2ParametersSucceeds() {
		var property = new JqwikProperty()
						   .withGeneration(PropertyRunStrategy.GenerationMode.RANDOMIZED);

		PropertyRunResult result = property.forAll(
			Values.just(1),
			Values.just(2)
		).verify((i1, i2) -> {
			assertThat(i1).isEqualTo(1);
			assertThat(i2).isEqualTo(2);
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	@Example
	void propertyWith2ParametersFails() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(
			Values.just(1),
			Values.just(2)
		).verify((i1, i2) -> {
			fail("failed");
		});
		assertThat(result.isFailed()).isTrue();
	}

	@Example
	void propertyId() {
		var propertyWithDefaultId = new JqwikProperty();
		String defaultId = getClass().getName() + "#" + "propertyId";
		assertThat(propertyWithDefaultId.id()).isEqualTo(defaultId);

		var propertyWithExplicitId = new JqwikProperty("myId");
		assertThat(propertyWithExplicitId.id()).isEqualTo("myId");
	}

	@Example
	void propertyDefaultStrategyReporting() {
		var property = new JqwikProperty();
		PropertyRunStrategy strategy = property.strategy();

		assertThat(strategy.maxTries()).isEqualTo(100);
		assertThat(strategy.maxRuntime()).isEqualTo(Duration.ofMinutes(10));
		assertThat(strategy.shrinking()).isEqualTo(PropertyRunStrategy.ShrinkingMode.FULL);
		assertThat(strategy.generation()).isEqualTo(PropertyRunStrategy.GenerationMode.SMART);
		assertThat(strategy.edgeCases()).isEqualTo(PropertyRunStrategy.EdgeCasesMode.MIXIN);
	}

	@Example
	void generationMode_EXHAUSTIVE() {
		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			100, Duration.ofMinutes(10), null,
			List.of(),
			PropertyRunStrategy.ShrinkingMode.OFF,
			PropertyRunStrategy.GenerationMode.EXHAUSTIVE,
			PropertyRunStrategy.EdgeCasesMode.OFF
		);
		var property = new JqwikProperty(strategy);

		PropertyRunResult result = property.forAll(
			Numbers.integers().between(0, 3),
			Numbers.integers().between(1, 2)
		).verify((i1, i2) -> {
			// System.out.println(i1 + " " + i2);
			assertThat(i1).isBetween(0, 3);
			assertThat(i2).isBetween(1, 2);
		});

		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(8);
		assertThat(result.countChecks()).isEqualTo(8);
	}

	@Example
	void generationMode_SMART() {
		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			100, Duration.ofMinutes(10), RandomChoice.generateRandomSeed(),
			List.of(),
			PropertyRunStrategy.ShrinkingMode.OFF,
			PropertyRunStrategy.GenerationMode.SMART,
			PropertyRunStrategy.EdgeCasesMode.MIXIN
		);
		var property = new JqwikProperty(strategy);

		PropertyRunResult resultExhaustive = property.forAll(
			Numbers.integers().between(0, 3),
			Numbers.integers().between(1, 2)
		).verify((i1, i2) -> {});
		assertThat(resultExhaustive.isSuccessful()).isTrue();
		assertThat(resultExhaustive.countTries()).isEqualTo(8);

		// 100 * 2 > 100 => randomized generation
		PropertyRunResult resultRandomized = property.forAll(
			Numbers.integers().between(1, 100),
			Numbers.integers().between(0, 1)
		).verify((i1, i2) -> {});
		assertThat(resultRandomized.isSuccessful()).isTrue();
		assertThat(resultRandomized.countTries()).isEqualTo(100);
	}

	@Example
	void edgeCasesMode_MIXIN() {
		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			1000, Duration.ofMinutes(10), RandomChoice.generateRandomSeed(),
			List.of(),
			PropertyRunStrategy.ShrinkingMode.OFF,
			PropertyRunStrategy.GenerationMode.SMART,
			PropertyRunStrategy.EdgeCasesMode.MIXIN
		);
		var property = new JqwikProperty(strategy);

		List<Integer> values = Collections.synchronizedList(new ArrayList<>());
		PropertyRunResult resultExhaustive = property.forAll(Numbers.integers()).verify(values::add);
		assertThat(resultExhaustive.isSuccessful()).isTrue();
		assertThat(resultExhaustive.countTries()).isEqualTo(1000);

		assertThat(values).contains(0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE);
	}

	@Example
	void generationMode_SAMPLES() {
		var integers = Numbers.integers();
		var generator = SampleGenerator.from(integers.generator());
		var randomSampleSource = SampleSource.of(new RandomGenSource());
		List<SampleRecording> sampleRecordings = new ArrayList<>();
		List<Integer> sampleValues = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			generator.generate(randomSampleSource).ifPresent(sample -> {
				sampleRecordings.add(sample.recording());
				sampleValues.add((Integer) sample.values().get(0));
			});
		}

		// Add non-fitting recording, which should be ignored
		sampleRecordings.add(new SampleRecording(list(atom(0))));

		// Add sample with too many parts, which should be ignored
		sampleRecordings.add(new SampleRecording(
			atom(42, 1),
			atom(0)
		));

		// System.out.println(samples);
		// System.out.println(values);

		PropertyRunStrategy strategy = PropertyRunStrategy.create(
			1000, Duration.ofMinutes(10), RandomChoice.generateRandomSeed(),
			sampleRecordings,
			PropertyRunStrategy.ShrinkingMode.OFF,
			PropertyRunStrategy.GenerationMode.SAMPLES,
			PropertyRunStrategy.EdgeCasesMode.MIXIN
		);
		var property = new JqwikProperty(strategy);

		List<Integer> values = Collections.synchronizedList(new ArrayList<>());
		PropertyRunResult resultExhaustive = property.forAll(integers).verify(values::add);
		assertThat(resultExhaustive.countTries()).isEqualTo(sampleValues.size());
		assertThat(values).isEqualTo(sampleValues);
	}

}
