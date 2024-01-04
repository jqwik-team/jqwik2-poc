package jqwik2;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import jqwik2.internal.*;

import net.jqwik.api.*;

import static jqwik2.PerformanceTests.*;
import static org.assertj.core.api.Assertions.*;

class ConcurrentRunnerTests {

	@Example
	void runToEnd() {
		time(
			"runToEnd: newSingleThreadExecutor", 1,
			() -> runToEnd(Executors.newSingleThreadExecutor())
		);

		int threads = Thread.activeCount() + 1;
		time(
			"runToEnd: newFixedThreadPool(%d)".formatted(threads), 1,
			() -> runToEnd(Executors.newFixedThreadPool(threads))
		);

		time(
			"runToEnd: newCachedThreadPool", 1,
			() -> runToEnd(Executors.newCachedThreadPool())
		);

		time(
			"runToEnd: newVirtualThreadPerTaskExecutor", 1,
			() -> runToEnd(Executors.newVirtualThreadPerTaskExecutor())
		);

	}

	private static void runToEnd(ExecutorService service) {
		ConcurrentRunner runner = new ConcurrentRunner(service, Duration.ofSeconds(10));
		AtomicInteger counter = new AtomicInteger();
		List<ConcurrentRunner.Task> tasks = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			tasks.add(shutdown -> {
				sleepInThread(10);
				sleep(10);
				// System.out.println("Task " + counter.get() + " running");
				counter.incrementAndGet();
			});
		}
		runner.run(tasks.iterator());
		assertThat(counter.get()).isEqualTo(100);
	}

	private static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignore) {
		}
	}

	private static void sleepInThread(int millis) {
		long before = System.currentTimeMillis();
		BigDecimal pow = new BigDecimal(new Random().nextDouble());
		while(System.currentTimeMillis() - before < millis) {
			pow = new BigDecimal(new Random().nextDouble())
					  .multiply(pow);
		}
	}
}
