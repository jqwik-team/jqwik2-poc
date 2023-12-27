package jqwik2.api;

public class PropertyAbortedException extends RuntimeException {

	public PropertyAbortedException(String message) {
		super(message);
	}

	public PropertyAbortedException(String message, Throwable cause) {
		super(message, cause);
	}

	public PropertyAbortedException(Throwable cause) {
		super(cause);
	}
}
