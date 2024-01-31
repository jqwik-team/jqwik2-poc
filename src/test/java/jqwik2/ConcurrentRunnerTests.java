package jqwik2;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.internal.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

@PropertyDefaults(tries = 10)
class ConcurrentRunnerTests {

	public static final String NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR = "newVirtualThreadPerTaskExecutor";

	@Provide
	Arbitrary<Pair<String, Supplier<ExecutorService>>> services() {
		return Arbitraries.of(
			new Pair<>("newSingleThreadExecutor", () -> Executors.newSingleThreadExecutor()),
			new Pair<>("newFixedThreadPool", () -> Executors.newFixedThreadPool(2)),
			new Pair<>("newCachedThreadPool", () -> Executors.newCachedThreadPool()),
			new Pair<>(NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR, () -> Executors.newVirtualThreadPerTaskExecutor())
		);
	}

	@Property
	void runSuccessfullyToEnd(@ForAll("services") Pair<String, Supplier<ExecutorService>> pair) throws Exception {
		String name = pair.first();
		ExecutorService service = pair.second().get();
		PerformanceTesting.time(
			"runToEnd: %s".formatted(name), 1,
			() -> runAllTasksToEnd(service)
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

	@Property
	void runSuccessfullyWithTimeout(@ForAll("services") Pair<String, Supplier<ExecutorService>> pair) throws Exception {
		String name = pair.first();
		ExecutorService service = pair.second().get();

		PerformanceTesting.time("runWithTimeout: " + name, 1, () -> runSuccessfullyWithTimeout(service, name));
	}

	private static void runSuccessfullyWithTimeout(ExecutorService service, String name) {
		ConcurrentRunner runner = new ConcurrentRunner(service, Duration.ofSeconds(1));
		AtomicInteger counter = new AtomicInteger();
		List<ConcurrentRunner.Task> tasks = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			final int factor = i;
			tasks.add(shutdown -> {
				sleepInThread(75 * factor);
				sleep(75 * factor);
				// System.out.println("Task " + counter.get() + " running");
				counter.incrementAndGet();
			});
		}
		try {
			runner.run(tasks.iterator());
			fail("Expected TimeoutException");
		} catch (TimeoutException timeoutException) {
			// System.out.println("Timeout occurred: " + timeoutException.getMessage());
		}
		if (!name.equals(NEW_VIRTUAL_THREAD_PER_TASK_EXECUTOR)) {
			// For virtual threads we can't be sure that at least one task has already be completed
			assertThat(counter.get()).isGreaterThanOrEqualTo(1);
		}
	}

	@Property
	void runSuccessfullyWithShutdown(@ForAll("services") Pair<String, Supplier<ExecutorService>> pair) throws Exception {
		String name = pair.first();
		ExecutorService service = pair.second().get();

		PerformanceTesting.time("runWithShutdown: " + name, 1, () -> runSuccessfullyWithShutdown(service));
	}

	private static void runSuccessfullyWithShutdown(ExecutorService service) throws TimeoutException {
		ConcurrentRunner runner = new ConcurrentRunner(service, Duration.ofSeconds(10));
		AtomicInteger countAll = new AtomicInteger();
		AtomicInteger countShutdowns = new AtomicInteger();
		List<ConcurrentRunner.Task> tasks = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			tasks.add(shutdown -> {
				sleepInThread(10);
				sleep(10);
				countAll.incrementAndGet();
				if (countAll.get() >= 50 && countAll.get() % 3 == 0) {
					shutdown.shutdown();
					countShutdowns.incrementAndGet();
				}
			});
		}
		runner.run(tasks.iterator());

		// System.out.println("countAll      : " + countAll.get());
		// System.out.println("countShutdowns: " + countShutdowns.get());

		assertThat(countAll.get()).isLessThanOrEqualTo(100);
		assertThat(countShutdowns.get()).isGreaterThanOrEqualTo(1);
	}

	@Property
	void shutdownOnTimeoutEvenIfTasksAreStillBeingAdded(@ForAll("services") Pair<String, Supplier<ExecutorService>> pair) {
		ExecutorService service = pair.second().get();

		ConcurrentRunner runner = new ConcurrentRunner(service, Duration.ofMillis(500));

		AtomicInteger createdTasks = new AtomicInteger();
		AtomicInteger runTasks = new AtomicInteger();
		var taskIterator = new Iterator<ConcurrentRunner.Task>() {
			@Override
			public boolean hasNext() {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				if (createdTasks.getAndIncrement() < 50) {
					return true;
				}
				return false;
			}

			@Override
			public ConcurrentRunner.Task next() {
				return shutdown -> {
					runTasks.incrementAndGet();
				};
			}
		};

		try {
			runner.run(taskIterator);
			fail("Expected TimeoutException");
		} catch (TimeoutException timeoutException) {
			// System.out.println("Timeout occurred: " + timeoutException.getMessage());
		}

		// System.out.println("service: " + pair.first());
		// System.out.println("created tasks: " + createdTasks.get());
		// System.out.println("run tasks    : " + runTasks.get());

		// Approximately 10 tasks should be run before the timeout
		assertThat(runTasks.get()).isGreaterThan(5);
		assertThat(runTasks.get()).isLessThan(20);
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
