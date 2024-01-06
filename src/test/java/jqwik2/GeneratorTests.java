package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.recording.*;

import net.jqwik.api.*;

import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

@Group
class GeneratorTests {

	@Group
	class Filtering {
		@Example
		void mapIntsToStrings() {
			Generator<Integer> divisibleBy3 = new IntegerGenerator(-100, 100).filter(i -> i % 3 == 0);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 20; i++) {
				int value = divisibleBy3.generate(source);
				assertThat(value).isBetween(-100, 100);
				assertThat(value % 3).isEqualTo(0);
			}
		}

		@Example
		void filteredEdgeCases() {
			Generator<Integer> evenNumbers = new IntegerGenerator(-10, 100).filter(i -> i % 2 == 0);

			var values = EdgeCasesTests.collectAllEdgeCases(evenNumbers);
			assertThat(values).containsExactlyInAnyOrder(-10, 0, 100);
		}

		@Example
		void filteredExhaustiveGeneration() {
			Generator<Integer> evenNumbers = new IntegerGenerator(-10, 100).filter(i -> i % 2 == 0);
			ExhaustiveSource<?> exhaustive = evenNumbers.exhaustive().get();
			assertThat(exhaustive.maxCount()).isEqualTo(111L);

			var values = ExhaustiveGenerationTests.collectAll(exhaustive, evenNumbers);
			assertThat(values).hasSize(56);
			assertThat(values).contains(-10, 0, 100);
		}

	}

	@Group
	class Mapping {

		@Example
		void mapIntsToStrings() {
			Generator<String> numberStrings = new IntegerGenerator(-10, 100).map(Object::toString);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 20; i++) {
				String value = numberStrings.generate(source);
				assertThat(Integer.parseInt(value)).isBetween(-10, 100);
			}
		}

		@Example
		void mappedEdgeCases() {
			Generator<String> numberStrings = new IntegerGenerator(-10, 100).map(Object::toString);

			var values = EdgeCasesTests.collectAllEdgeCases(numberStrings);
			assertThat(values).containsExactlyInAnyOrder(
				"-10",
				"-1",
				"0",
				"1",
				"100"
			);
		}

		@Example
		void mappedExhaustiveGeneration() {
			Generator<String> numberStrings = new IntegerGenerator(-10, 100).map(Object::toString);

			var exhaustive = numberStrings.exhaustive();
			assertThat(exhaustive).isPresent();
			assertThat(exhaustive.get().maxCount()).isEqualTo(111);

			var values = ExhaustiveGenerationTests.collectAll(exhaustive.get(), numberStrings);
			assertThat(values).hasSize(111);
			assertThat(values).contains(
				"-10",
				"-1",
				"0",
				"1",
				"42",
				"99",
				"100"
			);
		}

	}

	@Group
	class Integrals {

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
	}

	@Group
	class Lists {

		@Example
		void listOfInts() {
			Generator<List<Integer>> listOfInts = new IntegerGenerator(-10, 100).list(0, 5);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				List<Integer> value = listOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeLessThanOrEqualTo(5);
			}
		}

		@Example
		void listOfCertainSize() {
			Generator<List<Integer>> listOfInts = new IntegerGenerator(-10, 100).list(1, 2);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				List<Integer> value = listOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeBetween(1, 2);
			}
		}

		@Example
		void listOfIntsWithEdgeCases() {
			Generator<List<Integer>> listOfInts = new IntegerGenerator(-100, 100).list(0, 5);
			Generator<List<Integer>> listWithEdgeCases = WithEdgeCasesDecorator.decorate(listOfInts, 0.5, 10);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 50; i++) {
				List<Integer> value = listWithEdgeCases.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeLessThanOrEqualTo(5);
			}
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
			Generator<List<Integer>> generator = ints.list(0, 5);

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
			ListGenerator<Integer> listOfInts = new ListGenerator<>(ints, 0, 5);

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
			Generator<List<Integer>> listOfInts = ints.list(0, 5);

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
