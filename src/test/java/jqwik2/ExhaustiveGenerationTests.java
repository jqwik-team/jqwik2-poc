package jqwik2;

import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.internal.*;
import jqwik2.internal.exhaustive.*;
import jqwik2.internal.generators.*;

import net.jqwik.api.*;

import static jqwik2.api.ExhaustiveSource.list;
import static jqwik2.api.ExhaustiveSource.*;
import static org.assertj.core.api.Assertions.*;

class ExhaustiveGenerationTests {

	@Group
	class ExhaustiveAtoms {

		@Example
		void exhaustiveAtom() {
			ExhaustiveChoice atom = (ExhaustiveChoice) ExhaustiveSource.choice(3).get();
			assertThat(atom.maxCount()).isEqualTo(4L);

			assertAtom(atom, 0);

			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 1);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 2);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 3);
			assertThat(atom.advance()).isFalse();
		}

		@Example
		void exhaustiveAtomWithRange() {
			ExhaustiveChoice atom = (ExhaustiveChoice) ExhaustiveSource.choice(new ExhaustiveChoice.Range(2, 5)).get();
			assertThat(atom.maxCount()).isEqualTo(4L);

			assertAtom(atom, 2);

			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 3);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 4);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 5);
			assertThat(atom.advance()).isFalse();
		}

		@Example
		void twoConcatenatedExhaustiveAtoms() {
			ExhaustiveChoice first = (ExhaustiveChoice) ExhaustiveSource.choice(2).get();
			ExhaustiveChoice second = (ExhaustiveChoice) ExhaustiveSource.choice(1).get();

			first.chain(second);
			assertThat(first.maxCount()).isEqualTo(6L);

			assertThat(first.current().choose(3)).isEqualTo(0);
			assertThat(second.current().choose(2)).isEqualTo(0);

			assertThat(first.advance()).isTrue();
			assertThat(first.current().choose(3)).isEqualTo(0);
			assertThat(second.current().choose(2)).isEqualTo(1);

			assertThat(first.advance()).isTrue();
			assertThat(first.current().choose(3)).isEqualTo(1);
			assertThat(second.current().choose(2)).isEqualTo(0);

			assertThat(first.advance()).isTrue();
			assertThat(first.current().choose(3)).isEqualTo(1);
			assertThat(second.current().choose(2)).isEqualTo(1);

			assertThat(first.advance()).isTrue();
			assertThat(first.current().choose(3)).isEqualTo(2);
			assertThat(second.current().choose(2)).isEqualTo(0);

			assertThat(first.advance()).isTrue();
			assertThat(first.current().choose(3)).isEqualTo(2);
			assertThat(second.current().choose(2)).isEqualTo(1);

			assertThat(first.advance()).isFalse();
		}

		@Example
		void exhaustiveAtomWithRanges() {
			ExhaustiveChoice atom = (ExhaustiveChoice) ExhaustiveSource.choice(range(2, 7)).get();
			assertThat(atom.maxCount()).isEqualTo(6L);

			assertAtom(atom, 2);
			atom.advance();
			atom.advance();
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 5);
			atom.advance();
			atom.advance();

			assertThat(atom.advance()).isFalse();
		}

		@Example
		void orAtom() {

			ExhaustiveOr atom = (ExhaustiveOr) ExhaustiveSource.or(
				choice(range(0, 1)),
				choice(range(0, 2)),
				choice(value(42))
			).get();

			assertThat(atom.maxCount()).isEqualTo(6L);
			assertAtom(atom, 0);

			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 1);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 0);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 1);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 2);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 42);
			assertThat(atom.advance()).isFalse();
		}

		@Example
		void generateSample() {
			IntegerGenerator ints1 = new IntegerGenerator(0, 10);
			IntegerGenerator ints2 = new IntegerGenerator(-5, 5);

			SampleGenerator sampleGenerator = SampleGenerator.from(ints1, ints2);

			Optional<IterableExhaustiveSource> optionalExhaustive = IterableExhaustiveSource.from(ints1, ints2);
			assertThat(optionalExhaustive).isPresent();

			optionalExhaustive.ifPresent(exhaustiveGenSource -> {
				assertThat(exhaustiveGenSource.maxCount()).isEqualTo(121);

				List<Sample> allSamples = new ArrayList<>();
				for (SampleSource multiGenSource : exhaustiveGenSource) {
					var optionalSample = sampleGenerator.generate(multiGenSource);
					optionalSample.ifPresent(allSamples::add);
				}
				assertThat(allSamples).hasSize(121);

				List<List<Object>> values = allSamples.stream().map(Sample::values).toList();
				assertThat(values).contains(List.of(0, -5));
				assertThat(values).contains(List.of(10, 5));

				// Second iteration
				AtomicInteger count = new AtomicInteger(0);
				for (SampleSource multiGenSource : exhaustiveGenSource) {
					var optionalSample = sampleGenerator.generate(multiGenSource);
					optionalSample.ifPresent(i -> count.incrementAndGet());
				}
				assertThat(count).hasValue(121);
			});
		}

		private static void assertAtom(ExhaustiveChoice exhaustiveAtom, int... expected) {
			GenSource.Choice fixed = exhaustiveAtom.current();
			assertAtom(expected, fixed);
		}

		private static void assertAtom(ExhaustiveOr exhaustiveAtom, int... expected) {
			GenSource.Choice fixed = (GenSource.Choice) exhaustiveAtom.current();
			assertAtom(expected, fixed);
		}

		private static void assertAtom(int[] expected, GenSource.Choice fixed) {
			for (int i = 0; i < expected.length; i++) {
				assertThat(fixed.choose(Integer.MAX_VALUE))
					.describedAs("Expected %d at position %d", expected[i], i)
					.isEqualTo(expected[i]);
			}
		}

	}

	@Group
	class ExhaustiveTrees {
		@Example
		void exhaustiveList() {
			ExhaustiveSource<?> list = ExhaustiveSource.list(2, choice(2)).get();
			assertThat(list.maxCount()).isEqualTo(9L);

			assertList(list, 0, 0);
			assertThat(list.advance()).isTrue();
			assertList(list, 0, 1);
			assertThat(list.advance()).isTrue();
			assertList(list, 0, 2);
			assertThat(list.advance()).isTrue();
			assertList(list, 1, 0);
			assertThat(list.advance()).isTrue();
			assertList(list, 1, 1);
			assertThat(list.advance()).isTrue();
			assertList(list, 1, 2);
			assertThat(list.advance()).isTrue();
			assertList(list, 2, 0);
			assertThat(list.advance()).isTrue();
			assertList(list, 2, 1);
			assertThat(list.advance()).isTrue();
			assertList(list, 2, 2);

			assertThat(list.advance()).isFalse();
		}

		@Example
		void exhaustiveEmptyList() {
			ExhaustiveSource<?> list = ExhaustiveSource.list(0, choice(2)).get();
			assertThat(list.maxCount()).isEqualTo(1L);
			assertThat(list.advance()).isFalse();
		}

		private void assertList(ExhaustiveSource<?> list, int... expected) {
			GenSource.List fixedList = (GenSource.List) list.current();
			for (int i = 0; i < expected.length; i++) {
				var atom = fixedList.nextElement().choice();
				assertThat(atom.choose(Integer.MAX_VALUE))
					.describedAs("Expected %d at position %d", expected[i], i)
					.isEqualTo(expected[i]);
			}
		}

		@Example
		void exhaustiveFlatMap() {
			ExhaustiveFlatMap exhaustiveFlatMap = (ExhaustiveFlatMap) ExhaustiveSource.flatMap(
				ExhaustiveSource.choice(2),
				head -> list(head.choice().choose(3), choice(2))
			).get();

			assertThat(exhaustiveFlatMap.maxCount()).isEqualTo(13L);
			assertFlatMap(exhaustiveFlatMap, 0);

			assertThat(exhaustiveFlatMap.advance()).isTrue();
			assertFlatMap(exhaustiveFlatMap, 1, 0);
			assertThat(exhaustiveFlatMap.advance()).isTrue();
			assertFlatMap(exhaustiveFlatMap, 1, 1);
			assertThat(exhaustiveFlatMap.advance()).isTrue();
			assertFlatMap(exhaustiveFlatMap, 1, 2);

			assertThat(exhaustiveFlatMap.advance()).isTrue();
			assertFlatMap(exhaustiveFlatMap, 2, 0, 0);
			exhaustiveFlatMap.advance();
			assertFlatMap(exhaustiveFlatMap, 2, 0, 1);
			exhaustiveFlatMap.advance();
			assertFlatMap(exhaustiveFlatMap, 2, 0, 2);

			exhaustiveFlatMap.advance();
			assertFlatMap(exhaustiveFlatMap, 2, 1, 0);
			exhaustiveFlatMap.advance();
			assertFlatMap(exhaustiveFlatMap, 2, 1, 1);
			exhaustiveFlatMap.advance();
			assertFlatMap(exhaustiveFlatMap, 2, 1, 2);

			exhaustiveFlatMap.advance();
			assertFlatMap(exhaustiveFlatMap, 2, 2, 0);
			exhaustiveFlatMap.advance();
			assertFlatMap(exhaustiveFlatMap, 2, 2, 1);
			assertThat(exhaustiveFlatMap.advance()).isTrue();
			assertFlatMap(exhaustiveFlatMap, 2, 2, 2);
			assertThat(exhaustiveFlatMap.advance()).isFalse();
		}

		private void assertFlatMap(ExhaustiveFlatMap exhaustiveFlatMap, int... expected) {
			GenSource.Tuple tuple = exhaustiveFlatMap.current();
			int head = tuple.nextValue().choice().choose(Integer.MAX_VALUE);

			assertThat(head)
				.describedAs("Expected %d as head", expected[0])
				.isEqualTo(expected[0]);

			GenSource.List list = tuple.nextValue().list();

			for (int i = 0; i < head; i++) {
				var atom = list.nextElement().choice();
				assertThat(atom.choose(Integer.MAX_VALUE))
					.describedAs("Expected %d at position %d", expected[i + 1], i)
					.isEqualTo(expected[i + 1]);
			}
		}
	}

	@Example
	void smallIntegers() {
		Generator<Integer> ints0to10 = new IntegerGenerator(0, 10);

		ExhaustiveSource<?> exhaustive = ints0to10.exhaustive().get();
		assertThat(exhaustive.maxCount()).isEqualTo(11L);

		List<Integer> allValues = collectAll(exhaustive, ints0to10);
		assertThat(allValues).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
	}

	@Example
	void generalIntegers() {
		Generator<Integer> ints = new IntegerGenerator(-10, 100);

		ExhaustiveSource<?> exhaustive = ints.exhaustive().get();
		assertThat(exhaustive.maxCount()).isEqualTo(111L);

		List<Integer> allValues = collectAll(exhaustive, ints);
		assertThat(allValues).hasSize(111);
		assertThat(allValues).contains(-10, 100, 0, -9, 99, 42);
	}

	@Example
	void lists() {
		Generator<Integer> ints0to10 = new IntegerGenerator(-1, 2);
		Generator<List<Integer>> lists = new ListGenerator<>(ints0to10, 0, 2);

		ExhaustiveSource<?> exhaustive = lists.exhaustive().get();
		assertThat(exhaustive.maxCount()).isEqualTo(21L);

		List<List<Integer>> allValues = collectAll(exhaustive, lists);
		assertThat(allValues).hasSize(21);
		assertThat(allValues).containsExactly(
			List.of(),
			List.of(0),
			List.of(1),
			List.of(2),
			List.of(-1),
			List.of(0, 0),
			List.of(0, 1),
			List.of(0, 2),
			List.of(0, -1),
			List.of(1, 0),
			List.of(1, 1),
			List.of(1, 2),
			List.of(1, -1),
			List.of(2, 0),
			List.of(2, 1),
			List.of(2, 2),
			List.of(2, -1),
			List.of(-1, 0),
			List.of(-1, 1),
			List.of(-1, 2),
			List.of(-1, -1)
		);
	}

	@Example
	void listsWithMinSize() {
		Generator<Integer> ints0to10 = new IntegerGenerator(-1, 2);
		Generator<List<Integer>> lists = new ListGenerator<>(ints0to10, 2, 2);

		ExhaustiveSource<?> exhaustive = lists.exhaustive().get();
		assertThat(exhaustive.maxCount()).isEqualTo(16L);

		List<List<Integer>> allValues = collectAll(exhaustive, lists);
		assertThat(allValues).hasSize(16);
		assertThat(allValues).containsExactly(
			List.of(0, 0),
			List.of(0, 1),
			List.of(0, 2),
			List.of(0, -1),
			List.of(1, 0),
			List.of(1, 1),
			List.of(1, 2),
			List.of(1, -1),
			List.of(2, 0),
			List.of(2, 1),
			List.of(2, 2),
			List.of(2, -1),
			List.of(-1, 0),
			List.of(-1, 1),
			List.of(-1, 2),
			List.of(-1, -1)
		);
	}

	@Example
	void generateSampleWithIntsAndLists() {
		Generator<Integer> ints1 = new IntegerGenerator(0, 10).filter(i -> i % 2 == 0);
		ListGenerator<Integer> lists2 = new ListGenerator<>(new IntegerGenerator(0, 4), 0, 2);

		SampleGenerator sampleGenerator = SampleGenerator.from(ints1, lists2);

		Optional<IterableExhaustiveSource> optionalExhaustive = IterableExhaustiveSource.from(ints1, lists2);
		assertThat(optionalExhaustive).isPresent();

		optionalExhaustive.ifPresent(exhaustiveGenSource -> {

			assertThat(exhaustiveGenSource.maxCount()).isEqualTo(341);

			List<Sample> allSamples = new ArrayList<>();
			for (SampleSource sampleSource : exhaustiveGenSource) {
				var optionalSample = sampleGenerator.generate(sampleSource);
				optionalSample.ifPresent(allSamples::add);
			}
			assertThat(allSamples).hasSameSizeAs(new HashSet<>(allSamples));
			assertThat(allSamples).hasSize(186);

			List<List<Object>> values = allSamples.stream().map(Sample::values).toList();
			assertThat(values).contains(List.of(0, List.of()));
			assertThat(values).contains(List.of(10, List.of()));
			assertThat(values).contains(List.of(0, List.of(0)));
			assertThat(values).contains(List.of(0, List.of(4)));
			assertThat(values).contains(List.of(10, List.of(0)));
			assertThat(values).contains(List.of(10, List.of(4)));
			assertThat(values).contains(List.of(0, List.of(0, 0)));
			assertThat(values).contains(List.of(10, List.of(4, 4)));

			// Second iteration
			AtomicInteger count = new AtomicInteger(0);
			for (SampleSource sampleSource : exhaustiveGenSource) {
				var optionalSample = sampleGenerator.generate(sampleSource);
				optionalSample.ifPresent(i -> count.incrementAndGet());
			}
			assertThat(count).hasValue(186);
		});
	}

	public static <T> List<T> collectAll(ExhaustiveSource<?> exhaustiveSource, Generator<T> generator) {
		List<T> allValues = new ArrayList<>();
		for (GenSource genSource : exhaustiveSource) {
			try {
				T value = generator.generate(genSource);
				allValues.add(value);
			} catch (CannotGenerateException ignore) {}
		}
		return allValues;
	}

}
