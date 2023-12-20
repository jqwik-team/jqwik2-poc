package jqwik2;

public interface GenSource {

	Atom atom();

	List list();

	Tree tree();

	interface Atom extends GenSource {

		/**
		 * Choose a value between 0 and max - 1.
		 * @param max A value between 0 and Integer.MAX_VALUE
		 * @return a choice between 0 and max - 1
		 */
		int choose(int max);
	}

	interface List extends GenSource {
		GenSource nextElement();
	}

	interface Tree extends GenSource {

		GenSource head();

		GenSource child();
	}
}

