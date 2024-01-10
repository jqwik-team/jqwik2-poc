package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

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

	@SuppressWarnings("OverlyLongMethod")
	public void run(Iterator<ConcurrentRunner.Task> taskIterator) throws TimeoutException {
		List<Throwable> uncaughtErrors = Collections.synchronizedList(new ArrayList<>());
		List<Future<?>> submittedTasks = Collections.synchronizedList(new ArrayList<>());
		Timer timer = new Timer();
		try (executorService) {
			Shutdown shutdown = executorService::shutdownNow;
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
					submittedTasks.add(executorService.submit(runnable));
				} catch (RejectedExecutionException ignore) {
					// This can happen when a task is submitted after
					// the executor service has been shut down due to a falsified sample.
				}
			}

			AtomicBoolean timeoutOccurred = new AtomicBoolean(false);
			scheduleAdditionTimeoutTask(timer, submittedTasks, timeoutOccurred);

			waitForFinishOrFail(executorService);

			if (!uncaughtErrors.isEmpty()) {
				throw uncaughtErrors.getFirst();
			}

			if (timeoutOccurred.get()) {
				throw new TimeoutException("Concurrent run timed out after " + timeout);
			}
		} catch (TimeoutException timeoutException) {
			throw timeoutException;
		} catch (Throwable throwable) {
			ExceptionSupport.throwAsUnchecked(throwable);
		} finally {
			timer.cancel();
		}
	}

	private void scheduleAdditionTimeoutTask(Timer timer, List<Future<?>> submittedTasks, AtomicBoolean timeoutOccurred) {
		// Timeout is already handled by waitForFinishOrFail(), but this can speed up task cancellation,
		// especially with Executors.newVirtualThreadPerTaskExecutor().
		// This is a performance optimization only. Remove if it causes problems.
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				submittedTasks.forEach(future -> future.cancel(true));
				timeoutOccurred.set(true);
			}
		}, timeout.toMillis());
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
