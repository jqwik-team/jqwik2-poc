package jqwik2;

import java.util.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.Shrinkable;
import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.recording.*;
import jqwik2.internal.shrinking.*;

import net.jqwik.api.*;

import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.tuple;
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

	@Example
	void chooseValueGenerator() {
		Generator<String> choices = BaseGenerators.choose(List.of("a", "b", "c"));

		GenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			String value = choices.generate(source);
			assertThat(value).isIn("a", "b", "c");
		}

		var edgeCases = EdgeCasesTests.collectAllEdgeCases(choices);
		assertThat(edgeCases).containsExactlyInAnyOrder("a", "c");

		var exhaustiveSource = choices.exhaustive();
		assertThat(exhaustiveSource).isPresent();
		assertThat(exhaustiveSource.get().maxCount()).isEqualTo(3);

		var values = ExhaustiveGenerationTests.collectAll(exhaustiveSource.get(), choices);
		assertThat(values).containsExactly("a", "b", "c");
	}

	@Example
	void chooseWithFrequencyGenerator() {
		Generator<String> choices = BaseGenerators.frequency(List.of(
			new Pair<>(1, "a"),
			new Pair<>(5, "b"),
			new Pair<>(10, "c")
		));

		GenSource source = new RandomGenSource("42");

		Map<String, Integer> counts = new HashMap<>();
		for (int i = 0; i < 50; i++) {
			String value = choices.generate(source);
			assertThat(value).isIn("a", "b", "c");
			counts.compute(value, (k, v) -> v == null ? 1 : v + 1);
		}

		assertThat(counts.get("a")).isLessThan(counts.get("b"));
		assertThat(counts.get("b")).isLessThan(counts.get("c"));

		var edgeCases = EdgeCasesTests.collectAllEdgeCases(choices);
		assertThat(edgeCases).containsExactlyInAnyOrder("a", "c");

		var exhaustiveSource = choices.exhaustive();
		assertThat(exhaustiveSource).isPresent();
		assertThat(exhaustiveSource.get().maxCount()).isEqualTo(3);

		var values = ExhaustiveGenerationTests.collectAll(exhaustiveSource.get(), choices);
		assertThat(values).containsExactly("a", "b", "c");
	}

	@Group
	class Filtering {
		@Example
		void mapIntsToStrings() {
			Generator<Integer> divisibleBy3 = BaseGenerators.integers(-100, 100).filter(i -> i % 3 == 0);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 20; i++) {
				int value = divisibleBy3.generate(source);
				assertThat(value).isBetween(-100, 100);
				assertThat(value % 3).isEqualTo(0);
			}
		}

		@Example
		void filteredEdgeCases() {
			Generator<Integer> evenNumbers = BaseGenerators.integers(-10, 100).filter(i -> i % 2 == 0);

			var values = EdgeCasesTests.collectAllEdgeCases(evenNumbers);
			assertThat(values).containsExactlyInAnyOrder(-10, 0, 100);
		}

		@Example
		void filteredExhaustiveGeneration() {
			Generator<Integer> evenNumbers = BaseGenerators.integers(-10, 100).filter(i -> i % 2 == 0);
			ExhaustiveSource<?> exhaustive = evenNumbers.exhaustive().get();
			assertThat(exhaustive.maxCount()).isEqualTo(111L);

			var values = ExhaustiveGenerationTests.collectAll(exhaustive, evenNumbers);
			assertThat(values).hasSize(56);
			assertThat(values).contains(-10, 0, 100);
		}

		@Example
		void filteredShrinking() {
			Generator<Integer> ints = BaseGenerators.integers(-1000, 1000);
			Generator<Integer> evenInts = ints.filter(i -> i % 2 == 0);

			GenSource source = RecordedSource.of(tuple(996, 1)); // -996

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
			Generator<String> numberStrings = BaseGenerators.integers(-10, 100).map(Object::toString);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 20; i++) {
				String value = numberStrings.generate(source);
				assertThat(Integer.parseInt(value)).isBetween(-10, 100);
			}
		}

		@Example
		void mappedEdgeCases() {
			Generator<String> numberStrings = BaseGenerators.integers(-10, 100).map(Object::toString);

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
			Generator<String> numberStrings = BaseGenerators.integers(-10, 100).map(Object::toString);

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
			Generator<List<Integer>> listOfInts = BaseGenerators.integers(5, 10).flatMap(
				size -> BaseGenerators.integers(-10, 10).list(size, size)
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
			Generator<List<Integer>> listOfInts = BaseGenerators.integers(0, 1).flatMap(
				size -> BaseGenerators.integers(0, 10).list(size, size)
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
			Generator<List<Integer>> listOfInts = BaseGenerators.integers(0, 2).flatMap(
				size -> BaseGenerators.integers(0, 3).list(size, size)
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
			Generator<List<Integer>> listOfInts = BaseGenerators.integers(0, 2).flatMap(
				size -> BaseGenerators.integers(0, 3).list(size, size)
			);

			// List.of(3, 3)
			GenSource source = RecordedSource.of(
				tuple(
					choice(2),
					tuple(choice(0), list(choice(3), choice(3)))
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
	class Combining {

		@Example
		void simpleCombinations() {
			Arbitrary<Integer> ints = Numbers.integers().between(1, 1000);
			Arbitrary<String> strings = Numbers.integers().between(1, 100).map(Object::toString);

			Generator<Integer> combined = BaseGenerators.combine(sampler -> {
				int anInt = sampler.draw(ints);
				String aString = sampler.draw(strings);
				return anInt + aString.length();
			});

			RandomGenSource source = new RandomGenSource("42");
			GenRecorder recorder = new GenRecorder(source);
			for (int i = 0; i < 20; i++) {
				Integer value = combined.generate(recorder);
				assertThat(value).isBetween(2, 1003);

				GenSource recorded = RecordedSource.of(recorder.recording());
				assertThat(combined.generate(recorded)).isEqualTo(value);
			}
		}

		@Example
		void embedded() {
			Arbitrary<Integer> ints = Numbers.integers().between(1, 1000);
			Generator<Integer> combined = BaseGenerators.combine(sampler -> {
				int anInt = sampler.draw(ints);
				return anInt + 1;
			});
			var lists = combined.list(1, 3);

			RandomGenSource source = new RandomGenSource("42");
			GenRecorder recorder = new GenRecorder(source);
			for (int i = 0; i < 20; i++) {
				List<Integer> value = lists.generate(recorder);

				GenSource recorded = RecordedSource.of(recorder.recording());
				assertThat(lists.generate(recorded)).isEqualTo(value);
			}
		}

		@Example
		void withEdgeCases() {
			Arbitrary<Integer> ints = Numbers.integers().between(-10, 1000);

			Generator<Integer> combined = BaseGenerators.combine(sampler -> {
				int anInt = sampler.draw(ints);
				return 2 * anInt;
			});

			// There are no direct edge cases for this generator
			var edgeCases = EdgeCasesTests.collectAllEdgeCases(combined);
			assertThat(edgeCases).isEmpty();

			// But edge cases can be generated by the used generators
			combined = WithEdgeCasesDecorator.decorate(combined, 0.9, 100);

			List<Integer> values = new ArrayList<>();
			RandomGenSource source = new RandomGenSource("42");
			for (int i = 0; i < 20; i++) {
				Integer value = combined.generate(source);
				values.add(value);
			}
			assertThat(values).contains(-20, -2, 0, 2, 2000);
		}

		@Example
		void withFlatMapping() {
			var sizes = Numbers.integers().between(5, 10);
			Generator<List<Integer>> listOfInts = BaseGenerators.combine(sampler -> {
				int size = sampler.draw(sizes);
				return sampler.draw(Numbers.integers().between(-10, 10).list().ofSize(size));
			});

			RandomGenSource source = new RandomGenSource("42");
			GenRecorder recorder = new GenRecorder(source);
			for (int i = 0; i < 20; i++) {
				List<Integer> value = listOfInts.generate(recorder);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeBetween(5, 10);

				GenSource recorded = RecordedSource.of(recorder.recording());
				// System.out.println("recorded=" + recorder.recording());
				assertThat(listOfInts.generate(recorded)).isEqualTo(value);
			}

		}

		@Example
		void shrinking() {
			var sizes = Numbers.integers().between(5, 10);
			Generator<List<Integer>> listOfInts = BaseGenerators.combine(sampler -> {
				int size = sampler.draw(sizes);
				return sampler.draw(Numbers.integers().between(-10, 10).list().ofSize(size));
			});

			// [0, 0, -10, -2, 0, 8, -6, 0, 5]
			Recording recording = deserialize(
				"t[" +
					"c[4]:" +
					"t[" +
					"c[0]:l[" +
					"t[c[0]:c[0]]:" +
					"t[c[0]:c[0]]:" +
					"t[c[10]:c[1]]:" +
					"t[c[2]:c[1]]:" +
					"t[c[0]:c[0]]:" +
					"t[c[8]:c[0]]:" +
					"t[c[6]:c[1]]:" +
					"t[c[0]:c[0]]:" +
					"t[c[5]:c[0]]" +
					"]" +
					"]" +
					"]");
			GenSource source = RecordedSource.of(recording);

			Shrinkable<Object> shrinkable = new ShrinkableGenerator<>(listOfInts).generate(source).asGeneric();
			FalsifiedSample falsifiedSample = new FalsifiedSample(new Sample(List.of(shrinkable)), null);

			Tryable tryable = Tryable.from(args -> {
				List<Integer> list = (List<Integer>) args.getFirst();
				return list.stream().mapToInt(i -> i).sum() == 0;
			});
			Shrinker shrinker = new Shrinker(falsifiedSample, tryable);

			FalsifiedSample best = falsifiedSample;
			while (true) {
				Optional<FalsifiedSample> next = shrinker.next();
				if (next.isEmpty()) {
					break;
				}
				assertThat(next.get()).isLessThan(best);
				best = next.get();
			}

			assertThat(best.sample().values().getFirst()).isEqualTo(List.of(0, 0, 0, 0, 1));
		}
	}

	@Group
	class Integrals {

		@Example
		void smallInts() {
			Generator<Integer> minus10to100 = BaseGenerators.integers(-10, 100);
			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				Integer value = minus10to100.generate(source);
				assertThat(value).isBetween(-10, 100);
				// System.out.println("value=" + value);
			}
		}

		@Example
		void positiveInts() {
			Generator<Integer> gen5to10 = BaseGenerators.integers(5, 10);
			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				Integer value = gen5to10.generate(source);
				assertThat(value).isBetween(5, 10);
				// System.out.println("value=" + value);
			}
		}

		@Example
		void negativeInts() {
			Generator<Integer> minus20to0 = BaseGenerators.integers(-20, 0);
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
			Generator<List<Integer>> listOfInts = BaseGenerators.integers(-10, 100).list(0, 5);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				List<Integer> value = listOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeLessThanOrEqualTo(5);
			}
		}

		@Example
		void listOfCertainSize() {
			Generator<List<Integer>> listOfInts = BaseGenerators.integers(-10, 100).list(1, 2);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				List<Integer> value = listOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeBetween(1, 2);
			}
		}

		@Example
		void listOfIntsWithEdgeCases() {
			Generator<List<Integer>> listOfInts = BaseGenerators.integers(-100, 100).list(0, 5);
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
			Generator<Set<Integer>> setOfInts = BaseGenerators.integers(-10, 100).set(0, 5);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				Set<Integer> value = setOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSizeLessThanOrEqualTo(5);
			}
		}

		@Example
		void setOfFixedSize() {
			Generator<Set<Integer>> setOfInts = BaseGenerators.integers(0, 15).set(10, 10);

			RandomGenSource source = new RandomGenSource("42");

			for (int i = 0; i < 10; i++) {
				Set<Integer> value = setOfInts.generate(source);
				// System.out.println("value=" + value);
				assertThat(value).hasSize(10);
			}
		}

		@Example
		void setEdgeCases() {
			Generator<Set<Integer>> setOfInts = BaseGenerators.integers(0, 15).set(0, 10);
			Set<Set<Integer>> edgeCases = EdgeCasesTests.collectAllEdgeCases(setOfInts);
			assertThat(edgeCases).containsExactlyInAnyOrder(
				Set.of(),
				Set.of(0),
				Set.of(15)
			);
		}

		@Example
		void setExhaustiveGeneration() {
			Generator<Set<Integer>> setOfInts = BaseGenerators.integers(0, 3).set(0, 2);

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
			Generator<Integer> generator = BaseGenerators.integers(-100, 100);
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
			Generator<Integer> generator = BaseGenerators.integers(5, 10);
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
			Generator<Integer> ints = BaseGenerators.integers(-10, 100);
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

			GenSource.Tuple tuple = recorder.tuple();
			tuple.nextValue().choice().choose(5);
			tuple.nextValue().choice().choose(5);

			GenSource.List list = tuple.nextValue().list();
			list.nextElement().choice().choose(10);
			list.nextElement().choice().choose(10);
			list.nextElement().choice().choose(10);

			// System.out.println(recorder.recording());

			assertThat(recorder.recording()).isEqualTo(
				Recording.tuple(
					choice(2),
					choice(1),
					list(choice(8), choice(8), choice(4))
				)
			);
		}
	}

	@Group
	class FromRecording {

		@Example
		void valid() {
			Generator<Integer> ints = BaseGenerators.integers(-10, 100);

			GenSource source = RecordedSource.of(tuple(10, 0));
			Integer value = ints.generate(source);
			assertThat(value).isEqualTo(10);
		}

		@Example
		void invalidRecording() {
			Generator<Integer> ints = BaseGenerators.integers(-10, 100);

			assertThatThrownBy(() -> {
				GenSource recorded = RecordedSource.of(Recording.tuple(100, 1));
				ints.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);

			assertThatThrownBy(() -> {
				GenSource recorded = RecordedSource.of(choice(100));
				ints.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);

		}

		@Example
		void exhaustedRecording() {
			Generator<Integer> ints = BaseGenerators.integers(0, 100);
			ListGenerator<Integer> listOfInts = new ListGenerator<>(ints, 0, 5);

			assertThatThrownBy(() -> {
				GenSource recorded = RecordedSource.of(tuple(
					choice(3), list(choice(10))
				));
				listOfInts.generate(recorded);
			}).isInstanceOf(CannotGenerateException.class);
		}

		@Example
		void exhaustedRecordingWithBackUp() {
			Generator<Integer> ints = BaseGenerators.integers(0, 100);
			Generator<List<Integer>> listOfInts = ints.list(0, 5);

			Recording recording = tuple(
				choice(3), list(choice(10))
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
