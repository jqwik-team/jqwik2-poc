package jqwik2;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;

/**
 * A function that can be used for a single try of a property.
 */
@FunctionalInterface
public interface Tryable extends Function<List<Object>, TryExecutionResult> {
}
