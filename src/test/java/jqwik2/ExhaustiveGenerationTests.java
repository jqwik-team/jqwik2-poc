package jqwik2;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.ExhaustiveGenerator;
import jqwik2.api.*;
import jqwik2.api.recording.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class ExhaustiveGenerationTests {

	@Group
	class ExhaustiveAtoms {

		@Example
		void singleExhaustiveChoice() {
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
			ExhaustiveAtom atom = new ExhaustiveAtom(2, 3, 4);
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
		void generateSample() {
			IntegerGenerator ints1 = new IntegerGenerator(0, 10);
			IntegerGenerator ints2 = new IntegerGenerator(-10, -1);

			SampleGenerator sampleGenerator = SampleGenerator.from(ints1, ints2);

			ExhaustiveAtom atom1 = new ExhaustiveAtom(11);
			ExhaustiveAtom atom2 = new ExhaustiveAtom(11);

			// TODO: This is not yet working
			//ExhaustiveGenSource exhaustiveGenSource = new ExhaustiveGenSource(atom1, atom2);

			MultiGenSource multiGenSource = MultiGenSource.of(atom1, atom2);

			Sample sample = sampleGenerator.generate(multiGenSource);

			System.out.println(sample);

		}

		private static void assertAtom(ExhaustiveAtom atom, int... expected) {
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

		ExhaustiveGenerator exhaustive = ints0to10.exhaustive();
		assertThat(exhaustive.maxCount()).hasValue(11L);

		List<Integer> allValues = collectAllEdgeCases(exhaustive, ints0to10);
		assertThat(allValues).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

	}

	private static <T> List<T> collectAllEdgeCases(Iterable<Recording> recordings, Generator<T> generator) {
		return StreamSupport.stream(recordings.spliterator(), false)
							.map(RecordedSource::new)
							.map(generator::generate)
							.collect(Collectors.toList());
	}

}
