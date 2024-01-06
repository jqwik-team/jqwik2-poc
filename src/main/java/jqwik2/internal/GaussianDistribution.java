package jqwik2.internal;

import jqwik2.api.*;

public class GaussianDistribution implements RandomChoice.Distribution {

	private final double borderSigma;

	public GaussianDistribution(double borderSigma) {
		if (borderSigma <= 0) {
			throw new IllegalArgumentException("borderSigma must be greater than 0");
		}
		this.borderSigma = borderSigma;
	}

	@Override
	public int nextInt(RandomChoice random, int maxExcluded) {
		while (true) {
			var gaussian = random.nextGaussian();
			double gaussianFactor = Math.abs(gaussian) / borderSigma;
			long value = (long) Math.floor(gaussianFactor * maxExcluded);
			if (value < maxExcluded) {
				return (int) value;
			}
		}
	}
}
