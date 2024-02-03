package jqwik2;

import java.util.*;

import jqwik2.api.recording.*;

import net.jqwik.api.*;

import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.tuple;
import static jqwik2.api.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

class RecordingTests {

	@Group
	class Serialization {

		@Example
		void empty() {
			String serialized = EMPTY.serialize();
			assertThat(serialized).isEqualTo("t[]");
			assertThat(Recording.deserialize(serialized))
				.isEqualTo(EMPTY);
		}

		@Example
		void atoms() {
			AtomRecording atom = atom(1);
			String serialized = atom.serialize();
			assertThat(serialized).isEqualTo("a[1]");
			assertThat(Recording.deserialize(serialized)).isEqualTo(atom);

			assertSerializeDeserialize(atom(2));
		}

		@Example
		void lists() {
			ListRecording list = list(atom(1), atom(2));
			String serialized = list.serialize();
			assertThat(serialized).isEqualTo("l[a[1]:a[2]]");
			assertThat(Recording.deserialize(serialized)).isEqualTo(list);

			assertSerializeDeserialize(list(atom(123)));
			assertSerializeDeserialize(list());
		}

		@Example
		void tuples() {
			TupleRecording tuple = tuple(1, 2);
			String serialized = tuple.serialize();
			assertThat(serialized).isEqualTo("t[a[1]:a[2]]");
			assertThat(Recording.deserialize(serialized)).isEqualTo(tuple);

			assertSerializeDeserialize(tuple(14));
			assertSerializeDeserialize(tuple(new Recording[0]));
		}

		@Example
		void nestedRecording() {
			TupleRecording nested = tuple(atom(13), list(
				atom(99),
				tuple(atom(3), list(atom(1), atom(2))),
				list(atom(11), tuple(atom(12), atom(13)))
			));

			assertSerializeDeserialize(nested);
		}

		@Example
		void sampleRecording() {
			SampleRecording sample = new SampleRecording(List.of(
				atom(1),
				atom(2),
				atom(3)
			));

			var serialized = sample.serialize();
			assertThat(serialized).isEqualTo("a[1]:a[2]:a[3]");

			assertThat(SampleRecording.deserialize(serialized)).isEqualTo(sample);
		}

		private void assertSerializeDeserialize(Recording recording) {
			assertThat(Recording.deserialize(recording.serialize())).isEqualTo(recording);
		}

	}

	@Example
	void compareRecording() {
		assertThat(atom(2)).isGreaterThan(atom(1));

		assertThat(
			atom(0).compareTo(list(atom(0)))
		).isEqualTo(0);
		assertThat(
			atom(0).compareTo(tuple(atom(0), atom(0)))
		).isEqualTo(0);


		assertThat(list(atom(0))).isLessThan(list(atom(1)));
		assertThat(list(atom(1))).isLessThan(list(atom(0), atom(0)));
		assertThat(
			list(atom(0)).compareTo(tuple(atom(0), atom(0)))
		).isEqualTo(0);

		assertThat(
			tuple(atom(0), atom(2))
		).isLessThan(
			tuple(atom(1), atom(1))
		);

		assertThat(
			tuple(atom(0), atom(2))
		).isLessThan(
			tuple(atom(2), atom(0))
		);

		assertThat(
			tuple(atom(1), atom(2))
		).isLessThan(
			tuple(atom(0), atom(0), atom(0))
		);
	}

	@Example
	void compareSampleRecordings() {
		SampleRecording sample = new SampleRecording(atom(2));

		assertThat(sample).isGreaterThan(new SampleRecording(atom(1)));
		assertThat(sample).isLessThan(new SampleRecording(atom(3)));
		assertThat(sample.compareTo(new SampleRecording(atom(2)))).isEqualTo(0);
	}

	@Example
	void equality() {
		assertThat(atom(1)).isEqualTo(atom(1));
		assertThat(atom(1)).isNotEqualTo(atom(2));

		assertThat(list(atom(1))).isEqualTo(list(atom(1)));
		assertThat(list(atom(1))).isNotEqualTo(list(atom(0)));

		assertThat(tuple(atom(1), atom(2))).isEqualTo(tuple(atom(1), atom(2)));
		assertThat(tuple(atom(1), atom(1))).isNotEqualTo(tuple(atom(1), atom(2)));
	}

	@Example
	void isomorphism() {
		assertThat(
			atom(1).isomorphicTo(atom(0))
		).isTrue();

		assertThat(
			list(atom(1)).isomorphicTo(list(atom(0)))
		).isTrue();
		assertThat(
			list(atom(1)).isomorphicTo(list(atom(1), atom(1)))
		).isTrue();
		assertThat(
			list(atom(0)).isomorphicTo(list(tuple(0)))
		).isFalse();

		assertThat(
			tuple(atom(0), atom(1)).isomorphicTo(tuple(atom(1), atom(2)))
		).isTrue();
		assertThat(
			tuple(atom(1), atom(1)).isomorphicTo(tuple(atom(1), list()))
		).isFalse();
	}

	@Example
	void isomorphismSampleRecording() {
		assertThat(
			new SampleRecording(atom(1))
				.isomorphicTo(new SampleRecording(atom(0)))
		).isTrue();

		assertThat(
			new SampleRecording(tuple(1))
				.isomorphicTo(new SampleRecording(tuple(1, 1)))
		).isFalse();

		assertThat(
			new SampleRecording(atom(1), atom(2))
				.isomorphicTo(new SampleRecording(atom(1)))
		).isFalse();
	}
}
