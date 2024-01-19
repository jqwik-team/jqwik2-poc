package jqwik2;

public class PerformanceTesting {

	public interface ThrowingRunnable {
		void run() throws Exception;
	}

	public static long time(String label, int count, ThrowingRunnable runnable) throws Exception {
		long start = System.currentTimeMillis();
		for (int i = 0; i < count; i++) {
			runnable.run();
		}
		long end = System.currentTimeMillis();
		var duration = end - start;
		System.out.printf("[%s] Time: %d ms%n", label, duration);
		return duration;
	}
}
