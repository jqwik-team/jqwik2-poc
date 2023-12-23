package jqwik2;

public enum ExecutionResult {

	/**
	 * Indicates that the execution of a property or try was
	 * <em>successful</em>.
	 */
	SUCCESSFUL,

	/**
	 * Indicates that the execution of a property or try was
	 * <em>aborted</em> before the actual property method could be run.
	 */
	ABORTED,

	/**
	 * Indicates that the execution of a property or try has
	 * <em>failed</em>.
	 */
	FAILED
}

