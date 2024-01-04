package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import jqwik2.internal.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ConcurrentRunnerTests {

	@Example
	void runToEnd() {
		runToEnd(Executors.newSingleThreadExecutor());
		runToEnd(Executors.newFixedThreadPool(Thread.activeCount() + 1));
	}

	private static void runToEnd(ExecutorService service) {
		ConcurrentRunner runner = new ConcurrentRunner(service, Duration.ofSeconds(1));
		AtomicInteger counter = new AtomicInteger();
		List<ConcurrentRunner.Task> tasks = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			tasks.add(shutdown -> {
				sleep(10);
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
}
