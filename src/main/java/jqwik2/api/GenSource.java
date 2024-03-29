package jqwik2.api;

public interface GenSource {

	@SuppressWarnings("unchecked")
	static <T extends GenSource> T any() {
		return (T) new AnyGenSource();
	}

	Choice choice();

	List list();

	Tuple tuple();

	interface Choice extends GenSource {

		/**
		 * Choose a value between 0 and maxExcluded - 1.
		 *
		 * @param maxExcluded A value between 0 and Integer.MAX_VALUE
		 * @return a choice between 0 and maxExcluded - 1
		 */
		int choose(int maxExcluded);

		/**
		 * Choose a value between 0 and maxExcluded - 1.
		 *
		 * @param maxExcluded  A value between 0 and Integer.MAX_VALUE
		 * @param distribution The random distribution to use
		 * @return a choice between 0 and maxExcluded - 1
		 */
		int choose(int maxExcluded, RandomChoice.Distribution distribution);

	}

	interface List extends GenSource {
		GenSource nextElement();
	}

	interface Tuple extends GenSource {
		GenSource nextValue();
	}

	class AnyGenSource implements GenSource, Choice, GenSource.List, GenSource.Tuple {
		@Override
		public Choice choice() {
			return this;
		}

		@Override
		public List list() {
			return this;
		}

		@Override
		public Tuple tuple() {
			return this;
		}

		@Override
		public int choose(int maxExcluded) {
			return 0;
		}

		@Override
		public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
			return 0;
		}

		@Override
		public GenSource nextElement() {
			return this;
		}

		@Override
		public GenSource nextValue() {
			return this;
		}
	}
}

