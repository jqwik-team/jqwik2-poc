package jqwik2.api.stateful;

import java.util.*;
import java.util.function.*;

import jqwik2.internal.stateful.*;

/**
 * A chain represents a series of states of type {@code T} in which the previous state
 * is somehow transformed into the next state. {@linkplain Transformer Transformers} are
 * used to transform a state into the next state.
 * The term chain is used in relation to mathematical concepts like Markov chains.
 *
 * <p>
 *     State instances can be mutable or immutable.
 * </p>
 *
 * <p>
 *     Chains can be generated through {@linkplain Chain#startWith(Supplier)}.
 * </p>
 *
 * @see Transformer
 *
 * @param <T> The type of state to be transformed in a chain
 */
public interface Chain<T> extends Iterator<T> {

	/**
	 * Create arbitrary for {@linkplain Chain chains} with a certain initial state.
	 *
	 * @param initialSupplier function to create the initial state object
	 * @param <T>             The type of state to be transformed through the chain.
	 * @return new arbitrary instance
	 */
	static <T> ChainArbitrary<T> startWith(Supplier<? extends T> initialSupplier) {
		return new DefaultChainArbitrary<>(initialSupplier);
	}

	/**
	 * Return list of all applied transformations as far as they have been iterated.
	 *
	 * <p>
	 *     For a chain that has not been iterated through this list is empty.
	 * </p>
	 *
	 * @return list of describing strings
	 */
	List<String> transformations();

	/**
	 * Return list of all used transformer instances.
	 *
	 * <p>
	 * Checking transformer instances - e.g. if they are of a certain implementation type -
	 * only makes sense if the transformer's description string is NOT set explicitly.
	 * </p>
	 *
	 * <p>
	 * For a chain that has not been run this list is always empty.
	 * </p>
	 *
	 * @return list of transformer instances
	 */
	List<Transformer<T>> transformers();

	/**
	 * The maximum number of transformations that a chain can go through.
	 *
	 * @return a number &gt;= 1
	 */
	int maxTransformations();

	/**
	 * The current value of the chain. Is empty before the first transformation.
	 *
	 * @return an optional value of type T
	 */
	Optional<T> current();

	/**
	 * Replay the chain from the beginning up to the current state.
	 *
	 * @return an iterator over the chain
	 */
	Iterator<T> replay();
}
