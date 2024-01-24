package jqwik2;

import java.util.*;

import jqwik2.api.Shrinkable;
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

	@Example
	void justGenerator() {
		Generator<Integer> just42 = BaseGenerators.just(42);

		GenSource source = GenSource.any();
		for (int i = 0; i < 10; i++) {
			Integer value = just42.generate(source);
			assertThat(value).isEqualTo(42);
		}

		var edgeCases = EdgeCasesTests.collectAllEdgeCases(just42);
		assertThat(edgeCases).containsExactly(42);

		var exhaustiveSource = just42.exhaustive();
		assertThat(exhaustiveSource).isPresent();
		assertThat(exhaustiveSource.get().maxCount()).isEqualTo(1);

		var values = ExhaustiveGenerationTests.collectAll(exhaustiveSource.get(), just42);
		assertThat(values).containsExactly(42);
	}

	@Example
	void createGenerator() {
		Generator<List<String>> createA = BaseGenerators.create(() -> List.of("a"));

		GenSource source = GenSource.any();
		for (int i = 0; i < 10; i++) {
			List<String> value = createA.generate(source);
			assertThat(value).isEqualTo(List.of("a"));
		}

		var edgeCases = EdgeCasesTests.collectAllEdgeCases(createA);
		assertThat(edgeCases).containsExactly(List.of("a"));

		var exhaustiveSource = createA.exhaustive();
		assertThat(exhaustiveSource).isPresent();
		assertThat(exhaustiveSource.get().maxCount()).isEqualTo(1);

		var values = ExhaustiveGenerationTests.collectAll(exhaustiveSource.get(), createA);
		assertThat(values).containsExactly(List.of("a"));
	}

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

		@Example
		void filteredShrinking() {
			IntegerGenerator ints = new IntegerGenerator(-1000, 1000);
			Generator<Integer> evenInts = ints.filter(i -> i % 2 == 0);

			GenSource source = RecordedSource.of(atom(996, 1)); // -996

			Shrinkable<Integer> shrinkable = new ShrinkableGenerator<>(evenInts).generate(source);

			shrinkable.shrink().forEach(s -> {
				assertThat(s.recording()).isLessThan(shrinkable.recording());
				assertThat(s.value() % 2).isEqualTo(0);
				// System.out.println("shrink recording: " + s.recording());
				// System.out.println("shrink value: " + s.value());
			});
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
	class FlatMapping {

		@Example
		void flatMapIntsToListOfInts() {
			Generator<List<Integer>> listOfInts = new IntegerGenerator(5, 10).flatMap(
				size -> new IntegerGenerator(-10, 10).list(size, size)
			);

			RandomGenSource source = new RandomGenSource("42");
			for (int i = 0; i < 20; i++) {
				GenRecorder recorder = new GenRecorder(source);
				List<Integer> value = listOfInts.generate(recorder);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeBetween(5, 10);

				GenSource recorded = RecordedSource.of(recorder.recording());
				assertThat(listOfInts.generate(recorded)).isEqualTo(value);
			}
		}

		@Example
		void flatMappedEdgeCases() {
			Generator<List<Integer>> listOfInts = new IntegerGenerator(0, 1).flatMap(
				size -> new IntegerGenerator(0, 10).list(size, size)
			);

			var values = EdgeCasesTests.collectAllEdgeCases(listOfInts);
			assertThat(values).containsExactlyInAnyOrder(
				List.of(),
				List.of(0),
				List.of(10)
			);
		}

		@Example
		void flatMappedExhaustiveGeneration() {
			Generator<List<Integer>> listOfInts = new IntegerGenerator(0, 2).flatMap(
				size -> new IntegerGenerator(0, 3).list(size, size)
			);

			var exhaustive = listOfInts.exhaustive();
			assertThat(exhaustive).isPresent();
			assertThat(exhaustive.get().maxCount()).isEqualTo(21);

			var values = ExhaustiveGenerationTests.collectAll(exhaustive.get(), listOfInts);
			assertThat(values).hasSize(21);
			assertThat(values).contains(
				List.of(),
				List.of(0),
				List.of(1),
				List.of(2),
				List.of(3),
				List.of(0, 0),
				List.of(3, 3)
			);
		}

		@Example
		void flatMappedExhaustiveDoesNotExists() {
			Generator<Integer> intsFromInts = new IntegerGenerator(0, 2).flatMap(
				size -> new IntegerGenerator(0, size) {
					@Override
					public Optional<ExhaustiveSource<?>> exhaustive() {
						return Optional.empty();
					}
				}
			);

			var exhaustive = intsFromInts.exhaustive();
			assertThat(exhaustive).isEmpty();
		}

		@Example
		void flatMapShrinking() {
			Generator<List<Integer>> listOfInts = new IntegerGenerator(0, 2).flatMap(
				size -> new IntegerGenerator(0, 3).list(size, size)
			);

			// List.of(3, 3)
			GenSource source = RecordedSource.of(
				tree(
					atom(2),
					tree(atom(0), list(atom(3), atom(3)))
				)
			);

			Shrinkable<List<Integer>> shrinkable = new ShrinkableGenerator<>(listOfInts).generate(source);
			// System.out.println("value: " + shrinkable.value());

			shrinkable.shrink().forEach(s -> {
				assertThat(s.recording()).isLessThan(shrinkable.recording());
				// System.out.println("shrink recording: " + s.recording());
				// System.out.println("shrink value: " + s.value());
			});

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

		@Example
		void intsWithGaussianDistribution() {
			IntegerGenerator allInts = new IntegerGenerator(Integer.MIN_VALUE, Integer.MAX_VALUE, new GaussianDistribution(3));
			// IntegerGenerator allInts = new IntegerGenerator(0, 1000, new GaussianDistribution(3));
			// IntegerGenerator allInts = new IntegerGenerator(-100000, -1, new GaussianDistribution(3));
			RandomGenSource source = new RandomGenSource("42");

			Map<Integer, Integer> histogram = new HashMap<>();
			for (int i = 0; i < 10000; i++) {
				GenRecorder recorder = new GenRecorder(source);
				int value = allInts.generate(recorder);

				// Add to histogram with groups from -50 to +50
				int groupFactor = Math.max(Math.abs(allInts.max()), Math.abs(allInts.min())) / 50;
				int key = value / groupFactor;
				histogram.compute(key, (k, v) -> v == null ? 1 : v + 1);

				// Check that recorded values can be regenerated
				GenSource recorded = RecordedSource.of(recorder.recording());
				assertThat(allInts.generate(recorded)).isEqualTo(value);
			}

			// Mind that 0 might be overrepresented in the histogram by factor of 2 (due to rounding)
			// printHistogram(histogram);
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
	class Sets {

		@Example
		void setsOfInts() {
			Generator<Set<Integer>> setOfInts = new IntegerGenerator(-10, 100).set(0, 5);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				Set<Integer> value = setOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeLessThanOrEqualTo(5);
			}
		}

		@Example
		void setOfFixedSize() {
			Generator<Set<Integer>> setOfInts = new IntegerGenerator(0, 15).set(10, 10);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				Set<Integer> value = setOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSize(10);
			}
		}

		@Example
		void setEdgeCases() {
			Generator<Set<Integer>> setOfInts = new IntegerGenerator(0, 15).set(0, 10);
			Set<Set<Integer>> edgeCases = EdgeCasesTests.collectAllEdgeCases(setOfInts);
			assertThat(edgeCases).containsExactlyInAnyOrder(
				Set.of(),
				Set.of(0),
				Set.of(15)
			);
		}

		@Example
		void setExhaustiveGeneration() {
			Generator<Set<Integer>> setOfInts = new IntegerGenerator(0, 3).set(0, 2);

			Optional<? extends ExhaustiveSource<?>> exhaustive = setOfInts.exhaustive();
			assertThat(exhaustive).isPresent();
			assertThat(exhaustive.get().maxCount()).isEqualTo(11);

			List<Set<Integer>> all = ExhaustiveGenerationTests.collectAll(exhaustive.get(), setOfInts);
			assertThat(all).hasSize(11);
			assertThat(all).containsExactlyInAnyOrder(
				Set.of(),
				Set.of(0),
				Set.of(1),
				Set.of(2),
				Set.of(3),
				Set.of(0, 1),
				Set.of(0, 2),
				Set.of(0, 3),
				Set.of(1, 2),
				Set.of(1, 3),
				Set.of(2, 3)
			);
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

				GenSource recorded = RecordedSource.of(recorder.recording());
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

				GenSource recorded = RecordedSource.of(recorder.recording());
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

				GenSource recorded = RecordedSource.of(recorder.recording());
				assertThat(generator.generate(recorded))
					.isEqualTo(value);
			}
		}

		@Example
		void recordTuple() {
			RandomGenSource source = new RandomGenSource("42");
			GenRecorder recorder = new GenRecorder(source);

			GenSource.Tuple tuple = recorder.tuple(3);
			tuple.get(0).atom().choose(5);
			tuple.get(1).atom().choose(5);

			GenSource.List list = tuple.get(2).list();
			list.nextElement().atom().choose(10);
			list.nextElement().atom().choose(10);
			list.nextElement().atom().choose(10);

			System.out.println(recorder.recording());

			assertThat(recorder.recording()).isEqualTo(
				Recording.tuple(
					atom(2),
					atom(1),
					list(atom(8), atom(8), atom(4))
				)
			);
		}
	}

	@Group
	class FromRecording {

		@Example
		void valid() {
			IntegerGenerator ints = new IntegerGenerator(-10, 100);

			GenSource source = RecordedSource.of(atom(10, 0));
			Integer value = ints.generate(source);
			assertThat(value).isEqualTo(10);
		}

		@Example
		void invalidRecording() {
			IntegerGenerator ints = new IntegerGenerator(-10, 100);

			assertThatThrownBy(() -> {
				GenSource recorded = RecordedSource.of(atom(100, 1));
				ints.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);

			assertThatThrownBy(() -> {
				GenSource recorded = RecordedSource.of(atom(100));
				ints.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);

		}

		@Example
		void exhaustedRecording() {
			IntegerGenerator ints = new IntegerGenerator(0, 100);
			ListGenerator<Integer> listOfInts = new ListGenerator<>(ints, 0, 5);

			assertThatThrownBy(() -> {
				GenSource recorded = RecordedSource.of(tree(
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
			GenSource recordedSource = RecordedSource.of(recording, backUpSource);
			GenRecorder recorder = new GenRecorder(recordedSource);

			List<Integer> value = listOfInts.generate(recorder);
			// System.out.println("value=     " + value);
			// System.out.println("recording= " + recorder.recording());
		}

	}

}
