package jqwik2.api;

public interface GenSource {

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
}

