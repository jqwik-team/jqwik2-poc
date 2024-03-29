package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.support.*;

public class ConcurrentRunner implements TaskRunner {

	private final ExecutorService executorService;
	private final Duration maxRuntime;

	public ConcurrentRunner(ExecutorService executorService, Duration maxRuntime) {
		this.executorService = executorService;
		this.maxRuntime = maxRuntime;
	}

	@Override
	@SuppressWarnings("OverlyLongMethod")
	public void run(Iterator<ConcurrentRunner.Task> taskIterator) throws TimeoutException {
		List<Throwable> uncaughtErrors = Collections.synchronizedList(new ArrayList<>());
		List<Future<?>> submittedTasks = Collections.synchronizedList(new ArrayList<>());
		Timer timer = new Timer();
		try (executorService) {
			AtomicBoolean timeoutOccurred = new AtomicBoolean(false);
			scheduleTimeoutTask(timer, executorService, submittedTasks, timeoutOccurred);

			Shutdown shutdown = executorService::shutdownNow;
			while (taskIterator.hasNext()) {
				if (timeoutOccurred.get()) {
					break;
				}
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

			waitForFinishOrFail(executorService);

			if (!uncaughtErrors.isEmpty()) {
				throw uncaughtErrors.getFirst();
			}

			if (timeoutOccurred.get()) {
				throw new TimeoutException("Concurrent run timed out after " + maxRuntime);
			}
		} catch (Throwable throwable) {
			ExceptionSupport.throwAsUnchecked(throwable);
		} finally {
			timer.cancel();
		}
	}

	private void scheduleTimeoutTask(Timer timer, ExecutorService executorService, List<Future<?>> submittedTasks, AtomicBoolean timeoutOccurred) {
		// Timeout is also handled by waitForFinishOrFail(), but that won't work during task creation
		if (maxRuntime.isZero()) {
			return;
		}
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timeoutOccurred.set(true);
				executorService.shutdownNow();
				// This is an optimization to speed up shutdown with virtual threads:
				submittedTasks.forEach(future -> future.cancel(true));
			}
		}, maxRuntime.toMillis());
	}

	private void waitForFinishOrFail(ExecutorService executorService) throws TimeoutException {
		boolean timeoutOccurred = false;
		try {
			executorService.shutdown();
			if (!maxRuntime.isZero()) {
				timeoutOccurred = !executorService.awaitTermination(maxRuntime.toMillis(), TimeUnit.MILLISECONDS);
			}
		} catch (InterruptedException e) {
			executorService.shutdownNow();
		}
		if (timeoutOccurred) {
			executorService.shutdownNow();
			String message = "Concurrent run timed out after " + maxRuntime;
			throw new TimeoutException(message);
		}
	}

}
