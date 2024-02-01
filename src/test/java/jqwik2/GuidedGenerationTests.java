package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import org.opentest4j.*;

import net.jqwik.api.*;

import static jqwik2.internal.PropertyRunConfiguration.*;
import static org.assertj.core.api.Assertions.*;

class GuidedGenerationTests {

	@Property(tries = 10)
	void succeedingSequentialGuidance(@ForAll long seed) {
		List<Generator<?>> generators = List.of(BaseGenerators.integers(0, 100));
		AtomicInteger count = new AtomicInteger(0);
		Tryable tryable = Tryable.from(args -> {
			count.incrementAndGet();
			// System.out.println("args = " + args);
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

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
		};

		PropertyRunResult result = propertyCase.run(
			guided(
				() -> generate42Values,
				1000,
				false,
				Duration.ofSeconds(5),
				Executors::newSingleThreadExecutor
			)
		);

		assertThat(result.status()).isEqualTo(PropertyRunResult.Status.SUCCESSFUL);
		assertThat(count.get()).isEqualTo(42);
	}

	@Property(tries = 10)
	void failingSequentialGuidance(@ForAll long seed) {
		List<Generator<?>> generators = List.of(BaseGenerators.integers(0, 100));
		Tryable tryable = Tryable.from(args -> {
			int anInt = (int) args.get(0);
			// System.out.println("anInt = " + anInt);
			return anInt <= 90;
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

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
		};

		PropertyRunResult result = propertyCase.run(
			guided(
				() -> generateUntil91isGenerated,
				1000,
				false,
				Duration.ofSeconds(5),
				Executors::newSingleThreadExecutor
			)
		);

		// System.out.println("countNextSourceCalls = " + countNextSourceCalls.get());
		// System.out.println("result.countTries()  = " + result.countTries());

		assertThat(result.status()).isEqualTo(PropertyRunResult.Status.FAILED);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(91));
	}

	@Property(tries = 10)
	void overrideRunResultInGuidedGeneration(@ForAll long seed) {
		List<Generator<?>> generators = List.of(BaseGenerators.integers(0, 100));
		Tryable tryable = Tryable.from(args -> {
			// System.out.println("args = " + args);
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

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
			public PropertyRunResult overridePropertyResult(PropertyRunResult originalResult) {
				AssertionFailedError assertionError = new AssertionFailedError("Override");
				return originalResult.withStatus(PropertyRunResult.Status.FAILED)
									 .withFailureReason(assertionError);
			}
		};

		PropertyRunResult result = propertyCase.run(
			guided(
				() -> generate42Values,
				1000,
				false,
				Duration.ofSeconds(5),
				Executors::newSingleThreadExecutor
			)
		);

		assertThat(result.countTries()).isEqualTo(42);
		assertThat(result.status()).isEqualTo(PropertyRunResult.Status.FAILED);
		assertThat(result.failureReason()).isPresent().get().isInstanceOf(AssertionFailedError.class);
	}

}
