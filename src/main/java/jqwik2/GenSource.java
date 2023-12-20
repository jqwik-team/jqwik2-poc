package jqwik2;

public interface GenSource {

	Atom atom();

	List list();

	Tree tree();

	interface Atom extends GenSource {
		int choice(int max);
	}

	interface List extends GenSource {
		GenSource nextElement();
	}

	interface Tree extends GenSource {

		GenSource head();

		GenSource child();
	}
}

