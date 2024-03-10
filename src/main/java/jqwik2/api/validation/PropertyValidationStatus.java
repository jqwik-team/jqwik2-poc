package jqwik2.api.validation;

public enum PropertyValidationStatus {
	/**
	 * Indicates that the execution of a property was <em>successful</em>.
	 */
	SUCCESSFUL,

	/**
	 * Indicates that the execution of a property was
	 * <em>aborted</em> before the actual property method could be run.
	 */
	ABORTED,

	/**
	 * Indicates that the execution of a property has <em>failed</em>.
	 */
	FAILED
}
