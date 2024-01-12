package jqwik2.api.support;

public class ExceptionSupport {

	@SuppressWarnings("unchecked")
	public static <T extends Throwable, R> R throwAsUnchecked(Throwable t) throws T {
		throw (T) t;
	}

	public static void rethrowIfBlacklisted(Throwable exception) {
		if (exception instanceof OutOfMemoryError) {
			ExceptionSupport.throwAsUnchecked(exception);
		}
	}
}
