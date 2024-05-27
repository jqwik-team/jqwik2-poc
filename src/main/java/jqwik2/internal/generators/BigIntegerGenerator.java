package jqwik2.internal.generators;

import java.math.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class BigIntegerGenerator implements Generator<BigInteger> {
	private final BigInteger min;
	private final BigInteger max;
	private final RandomChoice.Distribution distribution;
	private final boolean isInIntegerRange;

	public BigIntegerGenerator(BigInteger min, BigInteger max) {
		this(min, max, RandomChoice.Distribution.UNIFORM);
	}

	// Currently only uniform distribution is supported
	private BigIntegerGenerator(BigInteger min, BigInteger max, RandomChoice.Distribution distribution) {
		if (min.compareTo(max) > 0) {
			throw new IllegalArgumentException("min must be smaller than or equal to max");
		}
		this.min = min;
		this.max = max;
		this.distribution = distribution;
		this.isInIntegerRange = min.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 && max.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0;
	}

	@Override
	public BigInteger generate(GenSource source) {
		if (isInIntegerRange) {
			var intValue = new IntegerGenerationSupport(source, distribution).chooseInt(min.intValueExact(), max.intValueExact());
			return BigInteger.valueOf(intValue);
		}
		throw new UnsupportedOperationException("Values outside Integer range are not supported yet");
	}

	@Override
	public Iterable<Recording> edgeCases() {
		if (isInIntegerRange) {
			return IntegerGenerationSupport.edgeCases(min.intValueExact(), max.intValueExact());
		}
		throw new UnsupportedOperationException("Values outside Integer range are not supported yet");
	}

	@Override
	public Optional<ExhaustiveSource<?>> exhaustive() {
		if (isInIntegerRange) {
			return IntegerGenerationSupport.exhaustive(min.intValueExact(), max.intValueExact());
		}
		throw new UnsupportedOperationException("Values outside Integer range are not supported yet");
	}

	public BigInteger max() {
		return max;
	}

	public BigInteger min() {
		return min;
	}
}
