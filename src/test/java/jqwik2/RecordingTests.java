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
			assertThat(Recording.deserialize(serialized)).isSameAs(EMPTY);
		}

		@Example
		void choices() {
			ChoiceRecording choice = choice(1);
			String serialized = choice.serialize();
			assertThat(serialized).isEqualTo("a[1]");
			assertThat(Recording.deserialize(serialized)).isEqualTo(choice);

			serialized = choice().serialize();
			assertThat(serialized).isEqualTo("a[]");
			assertThat(Recording.deserialize(serialized)).isEqualTo(choice());

			assertSerializeDeserialize(choice(2));
		}

		@Example
		void lists() {
			ListRecording list = list(choice(1), choice(2));
			String serialized = list.serialize();
			assertThat(serialized).isEqualTo("l[a[1]:a[2]]");
			assertThat(Recording.deserialize(serialized)).isEqualTo(list);

			assertSerializeDeserialize(list(choice(123)));
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
			TupleRecording nested = tuple(choice(13), list(
				choice(99),
				tuple(choice(3), list(choice(1), choice(2))),
				list(choice(11), tuple(choice(12), choice(13)))
			));

			assertSerializeDeserialize(nested);
		}

		@Example
		void sampleRecording() {
			SampleRecording sample = new SampleRecording(List.of(
				choice(1),
				choice(2),
				choice(3)
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
		assertThat(choice(2)).isEqualByComparingTo(choice(2));
		assertThat(choice(2)).isGreaterThan(choice(1));
		assertThat(choice(1)).isGreaterThan(choice());
		assertThat(choice()).isLessThan(choice(1));

		assertThat(
			choice(0).compareTo(list(choice(0)))
		).isEqualTo(0);
		assertThat(
			choice(0).compareTo(tuple(choice(0), choice(0)))
		).isEqualTo(0);


		assertThat(list(choice(0))).isLessThan(list(choice(1)));
		assertThat(list(choice(1))).isLessThan(list(choice(0), choice(0)));
		assertThat(
			list(choice(0)).compareTo(tuple(choice(0), choice(0)))
		).isEqualTo(0);

		assertThat(
			tuple(choice(0), choice(2))
		).isLessThan(
			tuple(choice(1), choice(1))
		);

		assertThat(
			tuple(choice(0), choice(2))
		).isLessThan(
			tuple(choice(2), choice(0))
		);

		assertThat(
			tuple(choice(1), choice(2))
		).isLessThan(
			tuple(choice(0), choice(0), choice(0))
		);
	}

	@Example
	void compareSampleRecordings() {
		SampleRecording sample = new SampleRecording(choice(2));

		assertThat(sample).isGreaterThan(new SampleRecording(choice(1)));
		assertThat(sample).isLessThan(new SampleRecording(choice(3)));
		assertThat(sample.compareTo(new SampleRecording(choice(2)))).isEqualTo(0);
	}

	@Example
	void equality() {
		assertThat(choice(1)).isEqualTo(choice(1));
		assertThat(choice(1)).isNotEqualTo(choice(2));

		assertThat(list(choice(1))).isEqualTo(list(choice(1)));
		assertThat(list(choice(1))).isNotEqualTo(list(choice(0)));

		assertThat(tuple(choice(1), choice(2))).isEqualTo(tuple(choice(1), choice(2)));
		assertThat(tuple(choice(1), choice(1))).isNotEqualTo(tuple(choice(1), choice(2)));
	}

	@Example
	void isomorphism() {
		assertThat(
			choice(1).isomorphicTo(choice(0))
		).isTrue();

		assertThat(
			list(choice(1)).isomorphicTo(list(choice(0)))
		).isTrue();
		assertThat(
			list(choice(1)).isomorphicTo(list(choice(1), choice(1)))
		).isTrue();
		assertThat(
			list(choice(0)).isomorphicTo(list(tuple(0)))
		).isFalse();

		assertThat(
			tuple(choice(0), choice(1)).isomorphicTo(tuple(choice(1), choice(2)))
		).isTrue();
		assertThat(
			tuple(choice(1), choice(1)).isomorphicTo(tuple(choice(1), list()))
		).isFalse();
	}

	@Example
	void isomorphismSampleRecording() {
		assertThat(
			new SampleRecording(choice(1))
				.isomorphicTo(new SampleRecording(choice(0)))
		).isTrue();

		assertThat(
			new SampleRecording(tuple(1))
				.isomorphicTo(new SampleRecording(tuple(1, 1)))
		).isFalse();

		assertThat(
			new SampleRecording(choice(1), choice(2))
				.isomorphicTo(new SampleRecording(choice(1)))
		).isFalse();
	}
}
