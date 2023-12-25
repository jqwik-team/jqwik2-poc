package jqwik2.api;

import java.util.random.*;

import jqwik2.*;

public interface RandomChoice {

	/**
	 * Return a random value, equally distributes between 0 and max - 1.
	 *
	 * @param maxExcluded The max choice to return
	 * @return A random value between 0 and max - 1
	 */
	int nextInt(int maxExcluded);

	/**
	 * Create a new source of randomness that is independent of this one,
	 * i.e. it does not share any state with this one.
	 *
	 * @return new source of randomness
	 */
	RandomChoice split();

	static RandomChoice create() {
		return new XORShiftRandomChoice();
	}

	static RandomChoice create(String seed) {
		try {
			return new XORShiftRandomChoice(seed);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(String.format("[%s] is not a valid random randomSeed.", seed));
		}
	}

	class XORShiftRandomChoice implements RandomChoice {
		private final RandomGenerator random;

		private XORShiftRandomChoice(String seed) {
			try {
				this.random = new XORShiftRandom(Long.parseLong(seed));
			} catch (NumberFormatException nfe) {
				String message = String.format("[%s] is not a valid random randomSeed.", seed);
				throw new IllegalArgumentException(message);
			}
		}

		private XORShiftRandomChoice() {
			this(new XORShiftRandom());
		}

		private XORShiftRandomChoice(XORShiftRandom random) {
			this.random = random;
		}

		@Override
		public int nextInt(int maxExcluded) {
			return random.nextInt(maxExcluded);
		}

		@Override
		public RandomChoice split() {
			return new XORShiftRandomChoice(new XORShiftRandom(random.nextLong()));
		}
	}

}
