package jqwik2;

public enum PropertyExecutionResult {

	/**
	 * Indicates that the execution of a property was
	 * <em>successful</em>.
	 */
	SUCCESSFUL,

	/**
	 * Indicates that the execution of a property was
	 * <em>aborted</em> before the actual property method could be run.
	 */
	ABORTED,

	/**
	 * Indicates that the execution of a property has
	 * <em>failed</em>.
	 */
	FAILED
}

