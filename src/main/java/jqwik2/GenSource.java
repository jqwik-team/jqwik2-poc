package jqwik2;

public interface GenSource {

	Atom atom();

	List list();

	Tree tree();

	/**
	 * Choose a value between min and max. Both included.
	 * min must be smaller than or equal to max.
	 *
	 * @param min A value between Integer.MIN_VALUE and Integer.MAX_VALUE
	 * @param max A value between min and Integer.MAX_VALUE
	 * @return a choice between min and max (included)
	 */
	default int chooseInt(int min, int max) {
		return GenSourceSupport.chooseInt(this, min, max);
	}

	interface Atom extends GenSource {

		/**
		 * Choose a value between 0 and max - 1.
		 *
		 * @param maxExcluded A value between 0 and Integer.MAX_VALUE
		 * @return a choice between 0 and max - 1
		 */
		int choose(int maxExcluded);

	}

	interface List extends GenSource {
		GenSource nextElement();
	}

	interface Tree extends GenSource {

		GenSource head();

		GenSource child();
	}
}

