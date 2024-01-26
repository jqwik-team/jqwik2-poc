package jqwik2.api.stateful;

import jqwik2.api.*;

public interface ChainArbitrary<T> extends Arbitrary<Chain<T>> {

	/**
	 * Allow an additional {@linkplain Transformation} on the generated chain.
	 *
	 * @param weight Determines the relative probability of a transformer to be chosen.
	 * @param transformation The {@linkplain Transformation provider} to add.
	 * @return new instance of arbitrary
	 */
	ChainArbitrary<T> withTransformation(int weight, Transformation<T> transformation);

	/**
	 * Allow an additional {@linkplain Transformation} with a default weight of 1.
	 *
	 * @param transformation The {@linkplain Transformation provider} to add.
	 * @return new instance of arbitrary
	 */
	default ChainArbitrary<T> withTransformation(Transformation<T> transformation) {
		return withTransformation(1, transformation);
	}

	/**
	 * Set the intended number of transformations of generated chains.
	 *
	 * <p>
	 * Setting {@code maxTransformations} to {@code -1} creates a potentially infinite chain.
	 * Such a chain will only end when a {@linkplain Transformer#endOfChain()} is applied.
	 * </p>
	 *
	 * @return new instance of arbitrary
	 */
	ChainArbitrary<T> withMaxTransformations(int maxTransformations);

	/**
	 * Create a potentially infinite chain.
	 * Such a chain will only end when a {@linkplain Transformer#endOfChain()} is applied.
	 *
	 * @return new instance of arbitrary
	 */
	default ChainArbitrary<T> infinite() {
		return withMaxTransformations(-1);
	}

}
