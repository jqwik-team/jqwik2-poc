package jqwik2;

import java.util.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.stateful.*;

import net.jqwik.api.*;

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

	private <T> List<T> collectAllValues(Chain<T> chain) {
		List<T> values = new ArrayList<>();
		while (chain.hasNext()) {
			values.add(chain.next());
		}
		return values;
	}

}
