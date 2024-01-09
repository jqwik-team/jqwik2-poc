package jqwik2.api;

public interface GenSource {

	@SuppressWarnings("unchecked")
	static <T extends GenSource> T any() {
		return (T) new AnyGenSource();
	}

	Atom atom();

	List list();

	Tree tree();

	interface Atom extends GenSource {

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
		 * @param maxExcluded A value between 0 and Integer.MAX_VALUE
		 * @param distribution The random distribution to use
		 * @return a choice between 0 and maxExcluded - 1
		 */
		int choose(int maxExcluded, RandomChoice.Distribution distribution);
	}

	interface List extends GenSource {
		GenSource nextElement();
	}

	interface Tree extends GenSource {

		GenSource head();

		GenSource child();
	}

	class AnyGenSource implements GenSource, GenSource.Atom, GenSource.List, GenSource.Tree {
		@Override
		public Atom atom() {
			return this;
		}

		@Override
		public List list() {
			return this;
		}

		@Override
		public Tree tree() {
			return this;
		}

		/**
		 * Choose a value between 0 and maxExcluded - 1.
		 *
		 * @param maxExcluded A value between 0 and Integer.MAX_VALUE
		 * @return a choice between 0 and maxExcluded - 1
		 */
		@Override
		public int choose(int maxExcluded) {
			return 0;
		}

		/**
		 * Choose a value between 0 and maxExcluded - 1.
		 *
		 * @param maxExcluded  A value between 0 and Integer.MAX_VALUE
		 * @param distribution The random distribution to use
		 * @return a choice between 0 and maxExcluded - 1
		 */
		@Override
		public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
			return 0;
		}

		@Override
		public GenSource nextElement() {
			return this;
		}

		@Override
		public GenSource head() {
			return this;
		}

		@Override
		public GenSource child() {
			return this;
		}
	}
}

