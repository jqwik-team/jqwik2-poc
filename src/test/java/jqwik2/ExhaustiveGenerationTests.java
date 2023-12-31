package jqwik2;

import java.util.*;
import java.util.List;

import jqwik2.api.*;

import net.jqwik.api.*;

import static jqwik2.ExhaustiveSource.*;
import static org.assertj.core.api.Assertions.*;

class ExhaustiveGenerationTests {

	@Group
	class ExhaustiveAtoms {

		@Example
		void choiceWithMax() {
			ExhaustiveChoice choice = new ExhaustiveChoice(3);
			assertThat(choice.maxCount()).isEqualTo(3L);
			assertThat(choice.choose(3)).isEqualTo(0);

			choice.advance();
			assertThat(choice.choose(3)).isEqualTo(1);
			choice.advance();
			assertThat(choice.choose(3)).isEqualTo(2);
			assertThat(choice.choose(2)).isEqualTo(0);

			assertThatThrownBy(() -> choice.advance())
				.isInstanceOf(Generator.NoMoreValues.class);

			choice.reset();
			assertThat(choice.choose(3)).isEqualTo(0);
		}

		@Example
		void choiceWithRange() {
			ExhaustiveChoice choice = new ExhaustiveChoice(2, 6);
			assertThat(choice.maxCount()).isEqualTo(4L);
			assertThat(choice.choose(4)).isEqualTo(2);

			choice.advance();
			assertThat(choice.choose(6)).isEqualTo(3);
			choice.advance();
			assertThat(choice.choose(6)).isEqualTo(4);
			assertThat(choice.choose(3)).isEqualTo(1);
			choice.advance();
			assertThat(choice.choose(6)).isEqualTo(5);

			assertThatThrownBy(() -> choice.advance())
				.isInstanceOf(Generator.NoMoreValues.class);

			choice.reset();
			assertThat(choice.choose(6)).isEqualTo(2);
		}

		@Example
		void twoConcatenatedExhaustiveChoices() {
			ExhaustiveChoice first = new ExhaustiveChoice(3);
			ExhaustiveChoice second = new ExhaustiveChoice(2);

			first.chain(second);
			assertThat(first.maxCount()).isEqualTo(6L);

			assertThat(first.choose(3)).isEqualTo(0);
			assertThat(second.choose(2)).isEqualTo(0);

			first.next();
			assertThat(first.choose(3)).isEqualTo(0);
			assertThat(second.choose(2)).isEqualTo(1);

			first.next();
			assertThat(first.choose(3)).isEqualTo(1);
			assertThat(second.choose(2)).isEqualTo(0);

			first.next();
			assertThat(first.choose(3)).isEqualTo(1);
			assertThat(second.choose(2)).isEqualTo(1);

			first.next();
			assertThat(first.choose(3)).isEqualTo(2);
			assertThat(second.choose(2)).isEqualTo(0);

			first.next();
			assertThat(first.choose(3)).isEqualTo(2);
			assertThat(second.choose(2)).isEqualTo(1);

			assertThatThrownBy(() -> first.next())
				.isInstanceOf(Generator.NoMoreValues.class);
		}

		@Example
		void exhaustiveAtom() {
			ExhaustiveAtom atom = ExhaustiveSource.atom(2, 3, 4);
			assertThat(atom.maxCount()).isEqualTo(24L);

			assertAtom(atom, 0, 0, 0);

			atom.next();
			assertAtom(atom, 0, 0, 1);
			atom.next();
			assertAtom(atom, 0, 0, 2);
			atom.next();
			assertAtom(atom, 0, 0, 3);
			atom.next();
			assertAtom(atom, 0, 1, 0);

			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();

			atom.next();
			assertAtom(atom, 1, 0, 0);

			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			atom.next();
			assertAtom(atom, 1, 2, 3);

			assertThatThrownBy(() -> atom.next())
				.isInstanceOf(Generator.NoMoreValues.class);

		}

		@Example
		void exhaustiveAtomWithRanges() {
			ExhaustiveAtom atom = ExhaustiveSource.atom(range(2, 4), range(3, 5), value(4));
			assertThat(atom.maxCount()).isEqualTo(4L);

			assertAtom(atom, 2, 3, 4);
			atom.next();
			atom.next();
			atom.next();
			assertAtom(atom, 3, 4, 4);

			assertThatThrownBy(() -> atom.next())
				.isInstanceOf(Generator.NoMoreValues.class);
		}

		@Example
		void atomAlternatives() {
			ExhaustiveAtom atom1 = ExhaustiveSource.atom(range(0, 2), value(0));
			ExhaustiveAtom atom2 = ExhaustiveSource.atom(range(0, 3), value(1));
			ExhaustiveAtom atom3 = ExhaustiveSource.atom(value(2), value(3));

			OrAtom atom = ExhaustiveSource.or(atom1, atom2, atom3);

			assertThat(atom.maxCount()).isEqualTo(6L);
			assertAtom(atom, 0, 0);

			atom.next();
			assertAtom(atom, 1, 0);
			atom.next();
			assertAtom(atom, 0, 1);
			atom.next();
			assertAtom(atom, 1, 1);
			atom.next();
			assertAtom(atom, 2, 1);
			atom.next();
			assertAtom(atom, 2, 3);
			assertThatThrownBy(() -> atom.next())
				.isInstanceOf(Generator.NoMoreValues.class);
		}

		@Example
		void generateSample() {
			IntegerGenerator ints1 = new IntegerGenerator(0, 10);
			IntegerGenerator ints2 = new IntegerGenerator(-10, -1);

			SampleGenerator sampleGenerator = SampleGenerator.from(ints1, ints2);

			IterableExhaustiveSource exhaustiveGenSource = IterableExhaustiveSource.from(ints1, ints2);

			assertThat(exhaustiveGenSource.maxCount()).isEqualTo(110L);

			List<Sample> allSamples = new ArrayList<>();
			for (MultiGenSource multiGenSource : exhaustiveGenSource) {
				Sample sample = sampleGenerator.generate(multiGenSource);
				// System.out.println(sample);
				allSamples.add(sample);
			}
			assertThat(allSamples).hasSize(110);

			List<List<Object>> values = allSamples.stream().map(Sample::values).toList();
			assertThat(values).contains(List.of(0, -1));
			assertThat(values).contains(List.of(10, -10));

			// Second iteration
			int count = 0;
			for (MultiGenSource multiGenSource : exhaustiveGenSource) {
				count++;
			}
			assertThat(count).isEqualTo(110);
		}

		private static void assertAtom(Atom atom, int... expected) {
			for (int i = 0; i < expected.length; i++) {
				assertThat(atom.choose(Integer.MAX_VALUE))
					.describedAs("Expected %d at position %d", expected[i], i)
					.isEqualTo(expected[i]);
			}
		}

	}

	@Example
	void smallIntegers() {
		Generator<Integer> ints0to10 = new IntegerGenerator(0, 10);

		ExhaustiveSource exhaustive = ints0to10.exhaustive().get();
		assertThat(exhaustive.maxCount()).isEqualTo(11L);

		List<Integer> allValues = collectAll(exhaustive, ints0to10);
		assertThat(allValues).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

	}

	private static <T> List<T> collectAll(ExhaustiveSource source, Generator<T> generator) {
		List<T> allValues = new ArrayList<>();
		while (true) {
			try {
				allValues.add(generator.generate(source));
				source.next();
			} catch (Generator.NoMoreValues noMoreValues) {
				break;
			}
		}
		return allValues;
	}

}
