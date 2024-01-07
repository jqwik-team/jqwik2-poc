package jqwik2;

import jqwik2.api.*;

public class BiasedDistribution implements RandomChoice.Distribution {
	private final double lambda;

	public BiasedDistribution(double bias) {
		if (bias <= 0) {
			throw new IllegalArgumentException("bias must be greater than 0");
		}
		this.lambda = bias;
	}

	@Override
	public int nextInt(RandomChoice random, int maxExcluded) {
		while (true) {
			var x = random.nextDouble();
			double biasFactor = Math.pow(x, lambda);
			long value = (long) Math.floor(biasFactor * maxExcluded);
			if (value < maxExcluded) {
				return (int) value;
			}
		}
	}
}
