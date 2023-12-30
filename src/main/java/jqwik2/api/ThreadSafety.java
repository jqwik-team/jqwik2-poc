package jqwik2.api;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;

// TODO: Add withLock() method
public interface ThreadSafety {

	default boolean isThreadSafe() {
		return false;
	}

	default Lock lock() {
		if (isThreadSafe()) {
			return new NullLock();
		}
		return new ReentrantLock();
	}

	/**
	 * A lock that does nothing. Used for thread safe objects.
	 */
	class NullLock implements Lock {

		@Override
		public void lock() {

		}

		@Override
		public void lockInterruptibly() throws InterruptedException {

		}

		@Override
		public boolean tryLock() {
			return true;
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) {
			return true;
		}

		@Override
		public void unlock() {

		}

		@Override
		public Condition newCondition() {
			throw new UnsupportedOperationException();
		}
	}

}
