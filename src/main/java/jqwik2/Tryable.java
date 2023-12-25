package jqwik2;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import org.opentest4j.*;

/**
 * A function that can be used for a single try of a property.
 */
@FunctionalInterface
public interface Tryable extends Function<List<Object>, TryExecutionResult> {

	default TryExecutionResult apply(Sample sample) {
		return apply(sample.values());
	}

	static Tryable from(Consumer<List<Object>> consumer) {
		return from(args -> {
			consumer.accept(args);
			return null;
		});
	}

	static Tryable from(Function<List<Object>, Object> function) {
		return (List<Object> parameters) -> {
			try {
				Object result = function.apply(parameters);
				if (result != null) {
					boolean isBooleanType = result.getClass().equals(Boolean.class);
					if (isBooleanType && result.equals(false)) {
						return new TryExecutionResult(TryExecutionResult.Status.FALSIFIED);
					}
				}
				return new TryExecutionResult(TryExecutionResult.Status.SATISFIED);
			} catch (AssertionError ae) {
				return new TryExecutionResult(TryExecutionResult.Status.FALSIFIED, ae);
			} catch (TestAbortedException tae) {
				return new TryExecutionResult(TryExecutionResult.Status.INVALID, tae);
			}
		};
	}
}
