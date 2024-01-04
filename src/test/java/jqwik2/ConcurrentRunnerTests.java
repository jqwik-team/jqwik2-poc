package jqwik2;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import jqwik2.internal.*;
import org.assertj.core.api.*;

import net.jqwik.api.*;

import static jqwik2.PerformanceTests.*;
import static org.assertj.core.api.Assertions.*;

class ConcurrentRunnerTests {

	@Example
	void runSuccessfullyToEnd() throws Exception {
		time(
			"runToEnd: newSingleThreadExecutor", 1,
			() -> runAllTasksToEnd(Executors.newSingleThreadExecutor())
		);

		int threads = Thread.activeCount() + 1;
		time(
			"runToEnd: newFixedThreadPool(%d)".formatted(threads), 1,
			() -> runAllTasksToEnd(Executors.newFixedThreadPool(threads))
		);

		time(
			"runToEnd: newCachedThreadPool", 1,
			() -> runAllTasksToEnd(Executors.newCachedThreadPool())
		);

		time(
			"runToEnd: newVirtualThreadPerTaskExecutor", 1,
			() -> runAllTasksToEnd(Executors.newVirtualThreadPerTaskExecutor())
		);
	}

	private static void runAllTasksToEnd(ExecutorService service) throws TimeoutException {
		ConcurrentRunner runner = new ConcurrentRunner(service, Duration.ofSeconds(10));
		AtomicInteger counter = new AtomicInteger();
		List<ConcurrentRunner.Task> tasks = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			tasks.add(shutdown -> {
				sleepInThread(10);
				sleep(10);
				// System.out.println("Task " + counter.get() + " running");
				counter.incrementAndGet();
			});
		}
		runner.run(tasks.iterator());
		assertThat(counter.get()).isEqualTo(50);
	}

	@Provide
	Arbitrary<Pair<String, ExecutorService>> services() {
		return Arbitraries.of(
			new Pair<>("newSingleThreadExecutor", Executors.newSingleThreadExecutor()),
			new Pair<>("newFixedThreadPool", Executors.newFixedThreadPool(2)),
			new Pair<>("newCachedThreadPool", Executors.newCachedThreadPool()),
			new Pair<>("newVirtualThreadPerTaskExecutor", Executors.newVirtualThreadPerTaskExecutor())
		);
	}

	@Property
	void runSuccessfullyWithTimeout(@ForAll("services") Pair<String, ExecutorService> pair) throws Exception {
		String name = pair.first();
		ExecutorService service = pair.second();

		time(name, 1, () -> runSuccessfullyWithTimeout(service));
	}

	private static void runSuccessfullyWithTimeout(ExecutorService service) {
		ConcurrentRunner runner = new ConcurrentRunner(service, Duration.ofSeconds(1));
		AtomicInteger counter = new AtomicInteger();
		List<ConcurrentRunner.Task> tasks = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			final int factor = i;
			tasks.add(shutdown -> {
				sleepInThread(100 * factor);
				sleep(100 * factor);
				// System.out.println("Task " + counter.get() + " running");
				counter.incrementAndGet();
			});
		}
		try {
			runner.run(tasks.iterator());
			fail("Expected TimeoutException");
		} catch (TimeoutException timeoutException) {
			System.out.println("Timeout occurred: " + timeoutException.getMessage());
		}
		assertThat(counter.get()).isGreaterThanOrEqualTo(2);
	}

	private static void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ignore) {
			// System.out.println("sleep interrupted");
			Thread.currentThread().interrupt();
		}
	}

	private static void sleepInThread(int millis) {
		long before = System.currentTimeMillis();
		BigDecimal pow = new BigDecimal(new Random().nextDouble());
		while (System.currentTimeMillis() - before < millis) {
			if (Thread.interrupted()) {
				// System.out.println("sleepInThread interrupted");
				break;
			}
			pow = new BigDecimal(new Random().nextDouble()).multiply(pow);
		}
	}
}
