package jqwik2.internal;

import java.util.*;
import java.util.concurrent.*;

public interface TaskRunner {
	@FunctionalInterface
	interface Task {
		void run(Shutdown shutdown) throws Throwable;
	}

	@FunctionalInterface
	interface Shutdown {
		void shutdown();
	}

	@SuppressWarnings("OverlyLongMethod")
	void run(Iterator<Task> taskIterator) throws TimeoutException;
}
