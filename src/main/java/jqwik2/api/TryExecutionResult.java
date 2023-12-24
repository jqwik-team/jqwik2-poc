package jqwik2.api;

public record TryExecutionResult(Status status, Throwable throwable) {

	public TryExecutionResult(Status status) {
		this(status, null);
	}

	/**
	 * Status of running a single try.
	 */
	public enum Status {
		/**
		 * Current try does not falsify the property
		 */
		SATISFIED,

		/**
		 * Current try does falsify the property
		 */
		FALSIFIED,

		/**
		 * Current try has invalid parameters
		 */
		INVALID
	}

}
