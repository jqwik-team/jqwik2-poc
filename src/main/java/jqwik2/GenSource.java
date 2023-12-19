package jqwik2;

public interface GenSource {

	Atom atom();

	List list();

	Tree tree();

	interface Atom extends GenSource {
		int choice(int max);
	}

	interface List extends GenSource {
		<T extends GenSource> T nextElement(Class<T> sourceType);
		default GenSource nextElement() {
			return nextElement(GenSource.class);
		}
	}

	interface Tree extends GenSource {
		<T extends GenSource> T head(Class<T> sourceType);

		default GenSource head() {
			return head(GenSource.class);
		}

		<T extends GenSource> T child(Class<T> sourceType);

		default GenSource child() {
			return child(GenSource.class);
		}
	}
}

