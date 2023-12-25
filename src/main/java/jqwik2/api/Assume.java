package jqwik2.api;

import java.util.function.*;

import org.opentest4j.*;

public class Assume {

	private Assume() {
	}

	/**
	 * If condition does not hold, the current try will be aborted,
	 * i.e., it will not be executed but not counted as a check.
	 *
	 * <p>
	 * Assumptions are typically positioned at the beginning of a tryable.
	 * </p>
	 *
	 * <p>
	 * A failing assumption in an example test (having a single try)
	 * will be reported as a skipped test.
	 * </p>
	 *
	 * @param condition Condition to make the assumption true
	 */
	public static void that(boolean condition) {
		if (!condition) {
			throw new TestAbortedException();
		}
	}

	/**
	 * If condition provided by conditionSupplier does not hold, the current try will be aborted,
	 * i.e., it will not be executed but not counted as a check.
	 *
	 * <p>
	 * Assumptions are typically positioned at the beginning of a property method.
	 *
	 * @param conditionSupplier supplier for condition to make assumption true
	 */
	public static void that(Supplier<Boolean> conditionSupplier) {
		that(conditionSupplier.get());
	}
}
