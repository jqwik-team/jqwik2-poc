package jqwik2;

import java.util.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.arbitraries.*;
import jqwik2.api.stateful.*;
import jqwik2.internal.*;

import net.jqwik.api.*;
import net.jqwik.api.statistics.*;

import static jqwik2.api.arbitraries.Values.*;
import static org.assertj.core.api.Assertions.*;

class StatefulTests {

	@Example
	void deterministicChain() {

		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 0)
				 .withTransformation(ignore -> just(Transformer.transform("+1", i -> i + 1)))
				 .withMaxTransformations(10);

		Chain<Integer> chain = chains.sample();

		assertThat(chain.current()).isEmpty();

		assertThat(collectAllValues(chain)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		assertThat(chain.transformations()).containsExactly(
			"+1", "+1", "+1", "+1", "+1", "+1", "+1", "+1", "+1", "+1"
		);
	}

	@Example
	void transformersAreCorrectlyReported() {
		Transformer<Integer> transformer = i -> i + 1;
		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 0)
				 .withTransformation(ignore -> just(transformer))
				 .withMaxTransformations(10);

		Chain<Integer> chain = chains.sample();

		assertThat(collectAllValues(chain)).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

		assertThat(chain.transformers().size()).isEqualTo(10);
		chain.transformers().forEach(t -> assertThat(t).isSameAs(transformer));
	}

	@Example
	void chainWithZeroMaxTransformations() {
		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 0)
				 .withTransformation(ignore -> just(Transformer.transform("+1", i -> i + 1)))
				 .withMaxTransformations(0);

		Chain<Integer> chain = chains.sample();

		assertThat(collectAllValues(chain)).containsExactly(0);
		assertThat(chain.transformations()).isEmpty();
	}

	@Example
	void infiniteChain() {
		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 0)
				 .withTransformation(supplier -> just(Transformer.transform("+1", i -> i + 1)))
				 .infinite();

		Chain<Integer> chain = chains.sample();

		int lastValue = -1;
		for (int i = 0; i < 1000; i++) {
			assertThat(chain.hasNext()).isTrue();
			lastValue = chain.next();
		}

		assertThat(lastValue).isEqualTo(999);
		assertThat(chain.current()).hasValue(999);
	}

	@Property(tries = 10)
	void chainWithSingleTransformation(@ForAll long seed) {
		Transformation<Integer> growBelow100OtherwiseShrink = intSupplier -> {
			int last = intSupplier.get();
			if (last < 100) {
				return Numbers.integers().between(0, 10).map(i -> t -> t + i);
			} else {
				return Numbers.integers().between(1, 10).map(i -> t -> t - i);
			}
		};
		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 1)
				 .withTransformation(growBelow100OtherwiseShrink)
				 .withMaxTransformations(50);

		Chain<Integer> chain = chains.generator().generate(new RandomGenSource(Long.toString(seed)));

		assertThat(chain.maxTransformations()).isEqualTo(50);
		assertThat(chain.transformations()).hasSize(0);

		int last = 1;
		while (chain.hasNext()) {
			int next = chain.next();
			if (last < 100) {
				assertThat(next).isGreaterThanOrEqualTo(last);
			} else {
				assertThat(next).isLessThan(last);
			}
			last = next;
		}

		assertThat(chain.transformations()).hasSize(50);
	}

	@Property(tries = 10)
	void chainWithSeveralTransformations(@ForAll long seed) {
		Transformation<Integer> growBelow50otherwiseShrink = intSupplier -> {
			int last = intSupplier.get();
			if (last < 50) {
				return Numbers.integers().between(10, 50)
							  .map(i -> Transformer.transform("+i=" + i, t -> t + i));
			} else {
				return Numbers.integers().between(2, 9)
							  .map(i -> Transformer.transform("-i=" + i, t -> t - i));
			}
		};

		Transformation<Integer> resetToValueBetween0andLastAbsolute = supplier -> {
			int last = supplier.get();
			return Numbers.integers().between(0, Math.abs(last))
						  .map(value -> Transformer.transform("=" + value, ignore -> value));
		};

		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 1)
				 .withTransformation(ignore -> just(Transformer.transform("-1", i -> i - 1)))
				 .withTransformation(growBelow50otherwiseShrink)
				 .withTransformation(resetToValueBetween0andLastAbsolute)
				 .withMaxTransformations(50);

		Chain<Integer> chain = chains.generator().generate(new RandomGenSource(Long.toString(seed)));

		assertThat(chain.maxTransformations()).isEqualTo(50);
		assertThat(chain.transformations()).hasSize(0);

		chain.forEachRemaining(ignore -> {});

		// System.out.println(chain.transformations());
		// System.out.println(chain.current());

		assertThat(chain.transformations()).hasSize(50);
	}

	@Property(tries = 500)
	@StatisticsReport(onFailureOnly = true)
	void useFrequenciesToChooseTransformers(@ForAll long seed) {

		Transformation<Integer> just1 = ignore -> just(t -> 1);
		Transformation<Integer> just2 = ignore -> just(t -> 2);

		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 0)
				 .withTransformation(1, just1)
				 .withTransformation(4, just2)
				 .withMaxTransformations(10);

		Chain<Integer> chain = chains.generator().generate(new RandomGenSource(Long.toString(seed)));

		while (chain.hasNext()) {
			Statistics.collect(chain.next());
		}

		Statistics.coverage(checker -> {
			checker.check(0).percentage(p -> p >= 9 && p <= 10); // Always 1 of 11
			checker.check(1).percentage(p -> p > 0 && p < 35);
			checker.check(2).percentage(p -> p > 55);
		});
	}

	private <T> List<T> collectAllValues(Chain<T> chain) {
		List<T> values = new ArrayList<>();
		while (chain.hasNext()) {
			values.add(chain.next());
		}
		return values;
	}

}
