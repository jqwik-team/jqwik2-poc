package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import jqwik2.api.support.*;

public class ConcurrentRunner {

	public interface Task {
		void run(Shutdown shutdown) throws Throwable;
	}

	public interface Shutdown {
		void shutdown();
	}

	private final ExecutorService executorService;
	private final Duration timeout;

	public ConcurrentRunner(ExecutorService executorService, Duration timeout) {
		this.executorService = executorService;
		this.timeout = timeout;
	}

	public void run(Iterator<ConcurrentRunner.Task> taskIterator) throws TimeoutException {
		List<Throwable> uncaughtErrors = Collections.synchronizedList(new ArrayList<>());
		try (executorService) {
			Shutdown shutdown = () -> executorService.shutdownNow();
			while (taskIterator.hasNext()) {
				Task task = taskIterator.next();
				try {
					Runnable runnable = () -> {
						try {
							task.run(shutdown);
						} catch (Throwable throwable) {
							uncaughtErrors.add(throwable);
						}
					};
					executorService.submit(runnable);
				} catch (RejectedExecutionException ignore) {
					// This can happen when a task is submitted after
					// the executor service has been shut down due to a falsified sample.
				}
			}
			waitForFinishOrFail(executorService);
			if (!uncaughtErrors.isEmpty()) {
				throw uncaughtErrors.getFirst();
			}
		} catch (TimeoutException timeoutException) {
			throw timeoutException;
		} catch (Throwable throwable) {
			ExceptionSupport.throwAsUnchecked(throwable);
		}
	}

	private void waitForFinishOrFail(ExecutorService executorService) throws TimeoutException {
		boolean timeoutOccurred = false;
		try {
			executorService.shutdown();
			timeoutOccurred = !executorService.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
		if (timeoutOccurred) {
			executorService.shutdownNow();
			String message = "Concurrent run timed out after " + timeout;
			throw new TimeoutException(message);
		}
	}

}
