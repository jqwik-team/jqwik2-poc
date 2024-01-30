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

	@Example
	void runGuidedProperty() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			int anInt = (int) args.get(0);
			// System.out.println("anInt = " + anInt);
			return anInt <= 90;
		});

		PropertyCase propertyCase = new PropertyCase(generators, tryable);

		AtomicInteger count = new AtomicInteger(0);

		// TODO: Convert that into a proper thread safe, synchronized GuidedGeneration
		//       Thereby find out why the same values are generated so often
		GuidedGeneration guidance = new GuidedGeneration() {
			private volatile Sample lastSample = null;
			private volatile boolean started = false;
			private final Iterator<SampleSource> iterator = new RandomGenSource("42").iterator();
			private final CountDownLatch latch = new CountDownLatch(1);

			@Override
			public synchronized boolean hasNext() {
				if (!started) {
					return true;
				}
				// try {
				// 	Thread.sleep(500);
				// } catch (InterruptedException e) {
				// 	throw new RuntimeException(e);
				// }
				try {
					if (!latch.await(5, TimeUnit.SECONDS)) {
						return false;
					}
					int last = (int) lastSample.values().getFirst();
					// System.out.println("last = " + last);
					return last != 91;
				} catch (InterruptedException ignore) {
					return false;
				}
			}

			@Override
			public synchronized SampleSource next() {
				count.incrementAndGet();
				if (!started) {
					started = true;
					return iterator.next();
				}
				return iterator.next();
			}

			@Override
			public void guide(TryExecutionResult result, Sample sample) {
				lastSample = sample;
				latch.countDown();
			}
		};

		PropertyRunResult result = propertyCase.run(
			guided(
				() -> guidance,
				1000,
				false,
				Duration.ofSeconds(10),
				Executors::newSingleThreadExecutor
			)
		);

		// System.out.println("count = " + count);
		// System.out.println("result.countTries() = " + result.countTries());

		assertThat(result.status()).isEqualTo(PropertyRunResult.Status.FAILED);
		FalsifiedSample smallest = result.falsifiedSamples().getFirst();
		assertThat(smallest.values()).isEqualTo(List.of(91));

	}

}
