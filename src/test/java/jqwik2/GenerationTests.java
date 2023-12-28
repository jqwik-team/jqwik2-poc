package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

import net.jqwik.api.*;

import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

class GenerationTests {

	@Example
	void smallInts() {
		Generator<Integer> minus10to100 = new IntegerGenerator(-10, 100);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			Integer value = minus10to100.generate(source);
			assertThat(value).isBetween(-10, 100);
			// System.out.println("value=" + value);
		}
	}

	@Example
	void positiveInts() {
		Generator<Integer> gen5to10 = new IntegerGenerator(5, 10);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			Integer value = gen5to10.generate(source);
			assertThat(value).isBetween(5, 10);
			// System.out.println("value=" + value);
		}
	}

	@Example
	void negativeInts() {
		Generator<Integer> minus20to0 = new IntegerGenerator(-20, 0);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 20; i++) {
			Integer value = minus20to0.generate(source);
			assertThat(value).isBetween(-20, 0);
			// System.out.println("value=" + value);
		}
	}

	@Example
	void listOfInts() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 5);

		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			List<Integer> value = listOfInts.generate(source);
			// System.out.println("value=" + value);
			assertThat(value).hasSizeLessThanOrEqualTo(5);
		}
	}

	@Example
	void listOfIntsWithEdgeCases() {
		IntegerGenerator ints = new IntegerGenerator(-100, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 5);
		Generator<List<Integer>> listWithEdgeCases = listOfInts.decorate(
			g -> new WithEdgeCasesDecorator<>(g, 0.5, 10)
		);

		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 50; i++) {
			List<Integer> value = listWithEdgeCases.generate(source);
			// System.out.println("value=" + value);
			assertThat(value).hasSizeLessThanOrEqualTo(5);
		}
	}

	@Group
	class WithRecorder {

		@Example
		void smallInts() {
			Generator<Integer> generator = new IntegerGenerator(-100, 100);
			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				GenRecorder recorder = new GenRecorder(source);
				Integer value = generator.generate(recorder);
				assertThat(value).isNotNull();

				RecordedSource recorded = new RecordedSource(recorder.recording());
				assertThat(generator.generate(recorded))
					.isEqualTo(value);
			}
		}

		@Example
		void positiveInts() {
			Generator<Integer> generator = new IntegerGenerator(5, 10);
			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				GenRecorder recorder = new GenRecorder(source);
				Integer value = generator.generate(recorder);
				assertThat(value).isNotNull();

				RecordedSource recorded = new RecordedSource(recorder.recording());
				assertThat(generator.generate(recorded))
					.isEqualTo(value);
			}
		}

		@Example
		void listOfInts() {
			IntegerGenerator ints = new IntegerGenerator(-10, 100);
			Generator<List<Integer>> generator = new ListGenerator<>(ints, 5);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				GenRecorder recorder = new GenRecorder(source);
				List<Integer> value = generator.generate(recorder);
				assertThat(value).isNotNull();

				RecordedSource recorded = new RecordedSource(recorder.recording());
				assertThat(generator.generate(recorded))
					.isEqualTo(value);
			}
		}
	}

	@Group
	class FromRecording {

		@Example
		void valid() {
			IntegerGenerator ints = new IntegerGenerator(-10, 100);

			GenSource source = new RecordedSource(atom(10, 0));
			Integer value = ints.generate(source);
			assertThat(value).isEqualTo(10);
		}

		@Example
		void invalidRecording() {
			IntegerGenerator ints = new IntegerGenerator(-10, 100);

			assertThatThrownBy(() -> {
				RecordedSource recorded = new RecordedSource(atom(100, 1));
				ints.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);

			assertThatThrownBy(() -> {
				RecordedSource recorded = new RecordedSource(atom(100));
				ints.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);

		}

		@Example
		void exhaustedRecording() {
			IntegerGenerator ints = new IntegerGenerator(0, 100);
			ListGenerator<Integer> listOfInts = new ListGenerator<>(ints, 5);

			assertThatThrownBy(() -> {
				RecordedSource recorded = new RecordedSource(tree(
					atom(3), list(atom(10))
				));
				listOfInts.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);
		}

		@Example
		void exhaustedRecordingWithBackUp() {
			IntegerGenerator ints = new IntegerGenerator(0, 100);
			ListGenerator<Integer> listOfInts = new ListGenerator<>(ints, 5);

			Recording recording = tree(
				atom(3), list(atom(10))
			);

			GenSource backUpSource = new RandomGenSource("42");
			RecordedSource recordedSource = new RecordedSource(recording, backUpSource);
			GenRecorder recorder = new GenRecorder(recordedSource);

			List<Integer> value = listOfInts.generate(recorder);
			// System.out.println("value=     " + value);
			// System.out.println("recording= " + recorder.recording());
		}

	}

}
