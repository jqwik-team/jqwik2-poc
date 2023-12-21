package jqwik2;

import net.jqwik.api.*;

import static jqwik2.recording.Recording.list;
import static jqwik2.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

class RecordingTests {

	@Example
	void recordingsCanBeCompared() {
		assertThat(atom(1, 0)).isLessThan(atom(1, 1));
		assertThat(atom(1)).isLessThan(atom(1, 1));
		assertThat(atom(0, 1)).isLessThan(atom(1, 1));
		assertThat(
			atom(0, 1).compareTo(atom(0, 1))
		).isEqualTo(0);
		assertThat(
			atom(0, 1).compareTo(list(atom(0)))
		).isEqualTo(0);
		assertThat(
			atom(0, 1).compareTo(tree(atom(0), atom(0)))
		).isEqualTo(0);


		assertThat(list(atom(1, 0))).isLessThan(list(atom(1, 1)));
		assertThat(list(atom(1))).isLessThan(list(atom(0), atom(0)));
		assertThat(
			list(atom(0)).compareTo(tree(atom(0), atom(0)))
		).isEqualTo(0);

		assertThat(
			tree(atom(0), atom(2))
		).isLessThan(
			tree(atom(1), atom(1))
		);

		assertThat(
			tree(atom(0), list(atom(2)))
		).isLessThan(
			tree(atom(1), atom(1))
		);

		assertThat(
			tree(atom(0), list(atom(2))).compareTo(
				tree(atom(0), atom(1))
			)
		).isEqualTo(0);
	}

	@Example
	void equality() {
		assertThat(atom(1, 0)).isEqualTo(atom(1, 0));
		assertThat(atom(1, 1)).isNotEqualTo(atom(1, 0));

		assertThat(list(atom(1, 0))).isEqualTo(list(atom(1, 0)));
		assertThat(list(atom(1, 1))).isNotEqualTo(list(atom(1, 0)));

		assertThat(tree(atom(1, 0), atom(1, 0))).isEqualTo(tree(atom(1, 0), atom(1, 0)));
		assertThat(tree(atom(1, 1), atom(1, 0))).isNotEqualTo(tree(atom(1, 0), atom(1, 0)));
	}

	@Example
	void isomorphism() {
		assertThat(
			atom(1, 0).isomorphicTo(atom(1, 1))
		).isTrue();
		assertThat(
			atom(1, 0).isomorphicTo(atom(1))
		).isFalse();

		assertThat(
			list().isomorphicTo(list(atom(0, 1), atom(0, 1)))
		).isTrue();
		assertThat(
			list(atom(1)).isomorphicTo(list(atom(0)))
		).isTrue();
		assertThat(
			list(atom(1)).isomorphicTo(list(atom(1), atom(1)))
		).isTrue();
		assertThat(
			list(atom(0)).isomorphicTo(list(atom(0, 0)))
		).isFalse();

		assertThat(
			tree(atom(1, 0), atom(1, 0)).isomorphicTo(tree(atom(1, 1), atom(1, 1)))
		).isTrue();
		assertThat(
			tree(atom(1, 0), atom(1, 0)).isomorphicTo(tree(atom(1, 1), list()))
		).isFalse();
	}
}
