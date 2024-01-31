package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

import net.jqwik.api.*;

import static jqwik2.internal.PropertyRunConfiguration.*;
import static org.assertj.core.api.Assertions.*;

class GuidedGenerationTests {

	@Property(tries = 10)
	void runSequentialGuidance(@ForAll long seed) {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
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

		System.out.println("countNextSourceCalls = " + countNextSourceCalls.get());
		System.out.println("result.countTries()  = " + result.countTries());

		assertThat(result.status()).isEqualTo(PropertyRunResult.Status.FAILED);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(91));

	}

}
