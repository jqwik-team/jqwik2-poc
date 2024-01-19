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
		String averageLabel = "";
		if (count > 1) {
			averageLabel = ", Average: %f ms (in %d tries)".formatted(duration / (double) count, count);
		}
		System.out.printf("[%s] Time: %d ms%s%n", label, duration, averageLabel);
		return duration;
	}
}
