package jqwik2.internal;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.support.*;

public class InMainThreadRunner implements TaskRunner {

	private final Duration maxRuntime;

	public InMainThreadRunner(Duration maxRuntime) {
		this.maxRuntime = maxRuntime;
	}

	@SuppressWarnings("OverlyLongMethod")
	public void run(Iterator<ConcurrentRunner.Task> taskIterator) throws TimeoutException {

		Timer timer = new Timer();
		AtomicBoolean timeoutOccurred = new AtomicBoolean(false);
		AtomicBoolean stopped = new AtomicBoolean(false);
		scheduleTimeoutTask(timer, timeoutOccurred, stopped, Thread.currentThread());

		try {
			ConcurrentRunner.Shutdown shutdown = () -> stopped.set(true);
			while (taskIterator.hasNext() && !stopped.get()) {
				ConcurrentRunner.Task task = taskIterator.next();
				task.run(shutdown);
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

	private void scheduleTimeoutTask(Timer timer, AtomicBoolean timeoutOccurred, AtomicBoolean stopped, Thread mainThread) {
		if (maxRuntime.isZero()) {
			return;
		}
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				timeoutOccurred.set(true);
				stopped.set(true);
				mainThread.interrupt();
			}
		}, maxRuntime.toMillis());
	}

}
