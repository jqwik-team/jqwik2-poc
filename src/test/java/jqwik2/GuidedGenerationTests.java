package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import org.opentest4j.*;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.*;

import static jqwik2.api.validation.PropertyValidationStatus.*;
import static jqwik2.internal.PropertyRunConfiguration.*;
import static org.assertj.core.api.Assertions.*;

import static net.jqwik.api.GenerationMode.*;

class GuidedGenerationTests {

	@Provide
	Arbitrary<Supplier<ExecutorService>> serviceSuppliers() {
		return Arbitraries.of(
			Executors::newCachedThreadPool,
			Executors::newVirtualThreadPerTaskExecutor,
			() -> Executors.newFixedThreadPool(4),
			null // Will use InMainThreadRunner
		);
	}

	@Property(tries = 10)
	void succeedingSequentialGuidance(
		@ForAll("serviceSuppliers") Supplier<ExecutorService> supplier,
		@ForAll long seed
	) {
		List<Generator<?>> generators = List.of(BaseGenerators.integers(0, 100));
		AtomicInteger count = new AtomicInteger(0);
		Tryable tryable = Tryable.from(args -> {
			count.incrementAndGet();
			// System.out.println("args = " + args);
		});

		PropertyRun propertyCase = new PropertyRun(generators, tryable);

		GuidedGeneration generate42Values = new SequentialGuidedGeneration() {
			volatile int count = 0;
			private final Iterator<SampleSource> iterator = new RandomGenSource(Long.toString(seed)).iterator();

			@Override
			protected SampleSource initialSource() {
				return iterator.next();
			}

			@Override
			protected SampleSource nextSource() {
				return iterator.next();
			}

			@Override
			protected boolean handleResult(TryExecutionResult result, Sample sample) {
				count++;
				return count < 42;
			}

			@Override
			protected boolean handleEmptyGeneration(SampleSource failingSource) {
				throw new IllegalStateException("Should not happen");
			}
		};

		PropertyRunResult result = propertyCase.run(
			guided(
				() -> generate42Values,
				1000, Duration.ofSeconds(5),
				false, false,
				supplier
			)
		);

		assertThat(result.status()).isEqualTo(SUCCESSFUL);
		assertThat(count.get()).isEqualTo(42);
	}

	@Property(tries = 10)
	void failingSequentialGuidance(
		@ForAll("serviceSuppliers") Supplier<ExecutorService> supplier,
		@ForAll long seed
	) {
		List<Generator<?>> generators = List.of(BaseGenerators.integers(0, 100));
		Tryable tryable = Tryable.from(args -> {
			int anInt = (int) args.get(0);
			// System.out.println("anInt = " + anInt);
			return anInt <= 90;
		});

		PropertyRun propertyCase = new PropertyRun(generators, tryable);

		AtomicInteger countNextSourceCalls = new AtomicInteger(0);

		GuidedGeneration generateUntil91isGenerated = new SequentialGuidedGeneration() {
			private final Iterator<SampleSource> iterator = new RandomGenSource(Long.toString(seed)).iterator();

			@Override
			protected SampleSource initialSource() {
				return iterator.next();
			}

			@Override
			protected SampleSource nextSource() {
				countNextSourceCalls.incrementAndGet();
				return iterator.next();
			}

			@Override
			protected boolean handleResult(TryExecutionResult result, Sample sample) {
				int lastInt = (int) sample.values().getFirst();
				// System.out.println("last = " + last);
				return lastInt != 91;
			}

			/**
			 * Handle the case that the previous sample generation was empty.
			 * Return true if the next sample generation should be started.
			 *
			 * @param failingSource
			 */
			@Override
			protected boolean handleEmptyGeneration(SampleSource failingSource) {
				throw new IllegalStateException("Should not happen");
			}
		};

		PropertyRunResult result = propertyCase.run(
			guided(
				() -> generateUntil91isGenerated,
				1000, Duration.ofSeconds(5),
				false, false,
				supplier
			)
		);

		// System.out.println("countNextSourceCalls = " + countNextSourceCalls.get());
		// System.out.println("result.countTries()  = " + result.countTries());

		assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(91));
	}

	@Property(generation = EXHAUSTIVE)
	void overrideRunResultInGuidedGeneration(@ForAll("serviceSuppliers") Supplier<ExecutorService> supplier) {
		List<Generator<?>> generators = List.of(BaseGenerators.integers(0, 100));
		Tryable tryable = Tryable.from(args -> {
			// System.out.println("args = " + args);
		});

		PropertyRun propertyCase = new PropertyRun(generators, tryable);

		GuidedGeneration generate42Values = new SequentialGuidedGeneration() {
			volatile int count = 0;
			private final Iterator<SampleSource> iterator = new RandomGenSource().iterator();

			@Override
			protected SampleSource initialSource() {
				return iterator.next();
			}

			@Override
			protected SampleSource nextSource() {
				return iterator.next();
			}

			@Override
			protected boolean handleResult(TryExecutionResult result, Sample sample) {
				count++;
				return count < 42;
			}

			@Override
			protected boolean handleEmptyGeneration(SampleSource failingSource) {
				throw new IllegalStateException("Should not happen");
			}

			@Override
			public Optional<Pair<PropertyValidationStatus, Throwable>> overrideValidationStatus(PropertyValidationStatus status) {
				AssertionFailedError assertionError = new AssertionFailedError("Override");
				return Optional.of(Pair.of(PropertyValidationStatus.FAILED, assertionError));
			}
		};

		PropertyRunResult result = propertyCase.run(
			guided(
				() -> generate42Values,
				1000, Duration.ofSeconds(5),
				false, false,
				supplier
			)
		);

		assertThat(result.countTries()).isEqualTo(42);
		assertThat(result.status()).isEqualTo(PropertyValidationStatus.FAILED);
		assertThat(result.failureReason()).isPresent().get().isInstanceOf(AssertionFailedError.class);
	}

}
