package jqwik2.api.support;

public class ExceptionSupport {

	public interface RunWithThrowable {
		void run() throws Throwable;
	}

	public interface SupplyWithThrowable<T> {
		T get() throws Throwable;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Throwable, R> R throwAsUnchecked(Throwable t) throws T {
		throw (T) t;
	}

	public static void runUnchecked(RunWithThrowable code) {
		try {
			code.run();
		} catch (Throwable t) {
			throwAsUnchecked(t);
		}
	}

	public static <T> T runUnchecked(SupplyWithThrowable<T> code) {
		try {
			return code.get();
		} catch (Throwable t) {
			return throwAsUnchecked(t);
		}
	}

	public static void rethrowIfBlacklisted(Throwable exception) {
		if (exception instanceof OutOfMemoryError) {
			ExceptionSupport.throwAsUnchecked(exception);
		}
	}
}
