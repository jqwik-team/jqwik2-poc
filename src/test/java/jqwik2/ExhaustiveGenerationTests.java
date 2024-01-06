package jqwik2;

import java.util.*;

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
		void choiceWithMax() {
			ExhaustiveChoice choice = new ExhaustiveChoice(2);
			assertThat(choice.maxCount()).isEqualTo(3L);
			assertThat(choice.choose(3)).isEqualTo(0);

			assertThat(choice.advance()).isTrue();
			assertThat(choice.choose(3)).isEqualTo(1);
			assertThat(choice.advance()).isTrue();
			assertThat(choice.choose(3)).isEqualTo(2);
			assertThat(choice.choose(2)).isEqualTo(0);

			assertThat(choice.advance()).isFalse();

			choice.reset();
			assertThat(choice.choose(3)).isEqualTo(0);
		}

		@Example
		void choiceWithRange() {
			ExhaustiveChoice choice = new ExhaustiveChoice(2, 5);
			assertThat(choice.maxCount()).isEqualTo(4L);
			assertThat(choice.choose(4)).isEqualTo(2);

			assertThat(choice.advance()).isTrue();
			assertThat(choice.choose(6)).isEqualTo(3);
			assertThat(choice.advance()).isTrue();
			assertThat(choice.choose(6)).isEqualTo(4);
			assertThat(choice.choose(3)).isEqualTo(1);
			assertThat(choice.advance()).isTrue();
			assertThat(choice.choose(6)).isEqualTo(5);

			assertThat(choice.advance()).isFalse();

			choice.reset();
			assertThat(choice.choose(6)).isEqualTo(2);
		}

		@Example
		void twoConcatenatedExhaustiveChoices() {
			ExhaustiveChoice first = new ExhaustiveChoice(2);
			ExhaustiveChoice second = new ExhaustiveChoice(1);

			first.chain(second);
			assertThat(first.maxCount()).isEqualTo(6L);

			assertThat(first.choose(3)).isEqualTo(0);
			assertThat(second.choose(2)).isEqualTo(0);

			assertThat(first.advance()).isTrue();
			assertThat(first.choose(3)).isEqualTo(0);
			assertThat(second.choose(2)).isEqualTo(1);

			assertThat(first.advance()).isTrue();
			assertThat(first.choose(3)).isEqualTo(1);
			assertThat(second.choose(2)).isEqualTo(0);

			assertThat(first.advance()).isTrue();
			assertThat(first.choose(3)).isEqualTo(1);
			assertThat(second.choose(2)).isEqualTo(1);

			assertThat(first.advance()).isTrue();
			assertThat(first.choose(3)).isEqualTo(2);
			assertThat(second.choose(2)).isEqualTo(0);

			assertThat(first.advance()).isTrue();
			assertThat(first.choose(3)).isEqualTo(2);
			assertThat(second.choose(2)).isEqualTo(1);

			assertThat(first.advance()).isFalse();
		}

		@Example
		void exhaustiveAtom() {
			ExhaustiveAtom atom = ExhaustiveSource.atom(1, 2, 3);
			assertThat(atom.maxCount()).isEqualTo(24L);

			assertAtom(atom, 0, 0, 0);

			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 0, 0, 1);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 0, 0, 2);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 0, 0, 3);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 0, 1, 0);

			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();

			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 1, 0, 0);

			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			atom.advance();
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 1, 2, 3);

			assertThat(atom.advance()).isFalse();
		}

		@Example
		void twoConcatenatedExhaustiveAtoms() {
			ExhaustiveAtom first = ExhaustiveSource.atom(2);
			ExhaustiveAtom second = ExhaustiveSource.atom(1);

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
			ExhaustiveAtom atom = ExhaustiveSource.atom(
				range(2, 3), range(3, 4), value(4)
			);
			assertThat(atom.maxCount()).isEqualTo(4L);

			assertAtom(atom, 2, 3, 4);
			atom.advance();
			atom.advance();
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 3, 4, 4);

			assertThat(atom.advance()).isFalse();
		}

		@Example
		void orAtom() {
			ExhaustiveAtom atom1 = ExhaustiveSource.atom(range(0, 1), value(0));
			ExhaustiveAtom atom2 = ExhaustiveSource.atom(range(0, 2), value(1));
			ExhaustiveAtom atom3 = ExhaustiveSource.atom(value(2), value(3));

			OrAtom atom = ExhaustiveSource.or(atom1, atom2, atom3);

			assertThat(atom.maxCount()).isEqualTo(6L);
			assertAtom(atom, 0, 0);

			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 1, 0);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 0, 1);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 1, 1);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 2, 1);
			assertThat(atom.advance()).isTrue();
			assertAtom(atom, 2, 3);
			assertThat(atom.advance()).isFalse();
		}

		@Example
		void generateSample() {
			IntegerGenerator ints1 = new IntegerGenerator(0, 10);
			IntegerGenerator ints2 = new IntegerGenerator(-5, 5);

			SampleGenerator sampleGenerator = SampleGenerator.from(ints1, ints2);

			IterableExhaustiveSource exhaustiveGenSource = IterableExhaustiveSource.from(ints1, ints2);

			assertThat(exhaustiveGenSource.maxCount()).isEqualTo(121);

			List<Sample> allSamples = new ArrayList<>();
			for (SampleSource multiGenSource : exhaustiveGenSource) {
				Sample sample = sampleGenerator.generate(multiGenSource);
				// System.out.println(sample);
				allSamples.add(sample);
			}
			assertThat(allSamples).hasSize(121);

			List<List<Object>> values = allSamples.stream().map(Sample::values).toList();
			assertThat(values).contains(List.of(0, -5));
			assertThat(values).contains(List.of(10, 5));

			// Second iteration
			int count = 0;
			for (SampleSource multiGenSource : exhaustiveGenSource) {
				count++;
			}
			assertThat(count).isEqualTo(121);
		}

		private static void assertAtom(ExhaustiveAtom exhaustiveAtom, int... expected) {
			GenSource.Atom fixed = exhaustiveAtom.current();
			assertAtom(expected, fixed);
		}

		private static void assertAtom(OrAtom exhaustiveAtom, int... expected) {
			GenSource.Atom fixed = exhaustiveAtom.current();
			assertAtom(expected, fixed);
		}

		private static void assertAtom(int[] expected, GenSource.Atom fixed) {
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
			ExhaustiveList list = ExhaustiveSource.list(2, atom(2));
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
			ExhaustiveList list = ExhaustiveSource.list(0, atom(2));
			assertThat(list.maxCount()).isEqualTo(1L);

			assertThat(list.size()).isEqualTo(0);

			assertThat(list.advance()).isFalse();
		}

		private void assertList(ExhaustiveList list, int... expected) {
			GenSource.List fixedList = (GenSource.List) list.current();
			for (int i = 0; i < expected.length; i++) {
				var atom = fixedList.nextElement().atom();
				assertThat(atom.choose(Integer.MAX_VALUE))
					.describedAs("Expected %d at position %d", expected[i], i)
					.isEqualTo(expected[i]);
			}
		}

		@Example
		void exhaustiveTree() {
			ExhaustiveTree tree = ExhaustiveSource.tree(
				ExhaustiveSource.atom(2),
				head -> list(head.atom().choose(3), atom(2))
			);

			assertThat(tree.maxCount()).isEqualTo(13L);
			assertTree(tree, 0);

			assertThat(tree.advance()).isTrue();
			assertTree(tree, 1, 0);
			assertThat(tree.advance()).isTrue();
			assertTree(tree, 1, 1);
			assertThat(tree.advance()).isTrue();
			assertTree(tree, 1, 2);

			assertThat(tree.advance()).isTrue();
			assertTree(tree, 2, 0, 0);
			tree.advance();
			assertTree(tree, 2, 0, 1);
			tree.advance();
			assertTree(tree, 2, 0, 2);

			tree.advance();
			assertTree(tree, 2, 1, 0);
			tree.advance();
			assertTree(tree, 2, 1, 1);
			tree.advance();
			assertTree(tree, 2, 1, 2);

			tree.advance();
			assertTree(tree, 2, 2, 0);
			tree.advance();
			assertTree(tree, 2, 2, 1);
			assertThat(tree.advance()).isTrue();
			assertTree(tree, 2, 2, 2);
			assertThat(tree.advance()).isFalse();
		}

		private void assertTree(ExhaustiveTree exhaustiveTree, int... expected) {
			GenSource.Tree tree = exhaustiveTree.current();
			int head = tree.head().atom().choose(Integer.MAX_VALUE);

			assertThat(head)
				.describedAs("Expected %d as head", expected[0])
				.isEqualTo(expected[0]);

			GenSource.List list = tree.child().list();

			for (int i = 0; i < head; i++) {
				var atom = list.nextElement().atom();
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

		ExhaustiveSource exhaustive = ints.exhaustive().get();
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

		IterableExhaustiveSource exhaustiveGenSource = IterableExhaustiveSource.from(ints1, lists2);

		assertThat(exhaustiveGenSource.maxCount()).isEqualTo(341);

		List<Sample> allSamples = new ArrayList<>();
		for (SampleSource multiGenSource : exhaustiveGenSource) {
			Sample sample = sampleGenerator.generate(multiGenSource);
			// System.out.println(sample);
			allSamples.add(sample);
		}
		assertThat(allSamples).hasSameSizeAs(new HashSet<>(allSamples));
		assertThat(allSamples).hasSize(217);

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
		int count = 0;
		for (SampleSource multiGenSource : exhaustiveGenSource) {
			count++;
		}
		assertThat(count).isEqualTo(217);
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
