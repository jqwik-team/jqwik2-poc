package jqwik2;

import java.util.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.*;
import jqwik2.api.stateful.*;
import jqwik2.internal.*;
import jqwik2.internal.recording.*;

import net.jqwik.api.*;
import net.jqwik.api.statistics.*;

import static jqwik2.api.arbitraries.Numbers.*;
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

		List<Integer> values = new ArrayList<>();
		while (chain.hasNext()) {
			chain.hasNext(); // is idempotent
			values.add(chain.next());
		}
		assertThat(values).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
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
		Transformation<Integer> growBelow100OtherwiseShrink = last -> {
			if (last < 100) {
				return integers().between(0, 10).map(i -> t -> t + i);
			} else {
				return integers().between(1, 10).map(i -> t -> t - i);
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
		Transformation<Integer> growBelow50otherwiseShrink = last -> {
			if (last < 50) {
				return integers().between(10, 50)
								 .map(i -> Transformer.transform("+i=" + i, t -> t + i));
			} else {
				return integers().between(2, 9)
								 .map(i -> Transformer.transform("-i=" + i, t -> t - i));
			}
		};

		Transformation<Integer> resetToValueBetween0andLastAbsolute = last -> {
			return integers().between(0, Math.abs(last))
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

	@Property(tries = 10)
	void transformationPreconditionsAreRespected(@ForAll long seed) {
		Transformation<List<Integer>> addRandomIntToList =
			ignore -> integers().between(0, 10)
								.map(i -> l -> {
									l.add(i);
									return l;
								});

		Transformation<List<Integer>> removeFirstElement =
			Transformation.<List<Integer>>when(last -> !last.isEmpty())
						  .provide(just(l -> {
							  l.remove(0);
							  return l;
						  }));

		ChainArbitrary<List<Integer>> chains =
			Chain.startWith(() -> (List<Integer>) new ArrayList<Integer>())
				 .withTransformation(addRandomIntToList)
				 .withTransformation(removeFirstElement)
				 .withMaxTransformations(13);

		Chain<List<Integer>> chain = chains.generator().generate(new RandomGenSource(Long.toString(seed)));
		chain.next(); // Ignore initial state

		List<Integer> last = new ArrayList<>();
		while (chain.hasNext()) {
			int lastSize = last.size();
			List<Integer> next = chain.next();
			assertThat(lastSize).isNotEqualTo(next.size());
			last = next;
		}
		assertThat(chain.transformations()).hasSize(13);
	}

	@Property(tries = 10)
	void noopTransformersAreIgnored(@ForAll long seed) {
		Transformation<Integer> addOne =
			ignore -> just(1).map(toAdd -> i -> i + toAdd);

		Transformation<Integer> justNoop = ignore -> just(Transformer.noop());
		Transformation<Integer> noopOrNoop = ignore -> of(Transformer.noop(), Transformer.noop());

		ChainArbitrary<Integer> chains =
			Chain.startWith(() -> 0)
				 .withTransformation(addOne)
				 .withTransformation(justNoop)
				 .withTransformation(noopOrNoop)
				 .withMaxTransformations(13);

		Chain<Integer> chain = chains.generator().generate(new RandomGenSource(Long.toString(seed)));

		int last = chain.next();
		while (chain.hasNext()) {
			int next = chain.next();
			assertThat(last + 1).isEqualTo(next);
			last = next;
		}
		assertThat(chain.transformations()).hasSize(13);
	}

	@Example
	void stopGenerationIfNoTransformerApplies() {
		Arbitrary<Chain<Integer>> chains =
			Chain.startWith(() -> 1)
				 .withTransformation(
					 Transformation.<Integer>when(ignore -> false)
								   .provide((Arbitrary<Transformer<Integer>>) null) // never gets here
				 )
				 .withTransformation(ignore -> just(Transformer.noop())) // noop() is ignored
				 .withMaxTransformations(50);

		Chain<Integer> chain = chains.sample();

		assertThatThrownBy(() -> {
			chain.forEachRemaining(ignore -> {});
		}).isInstanceOf(CannotGenerateException.class);
	}

	@Example
	void failToCreateGeneratorIfNoTransformersAreProvided() {
		Arbitrary<Chain<Integer>> chains = Chain.startWith(() -> 1).withMaxTransformations(50);

		assertThatThrownBy(() -> {
			chains.generator().generate(new RandomGenSource("42"));
		}).isInstanceOf(CannotGenerateException.class);
	}

	@Example
	void chainCanBeRegenerated() {

		Transformation<List<Integer>> addRandomIntToList =
			ignore -> integers().between(0, 10)
								.map(i -> l -> {
									l.add(i);
									return l;
								});

		Transformation<List<Integer>> removeFirstElement =
			Transformation.<List<Integer>>when(last -> !last.isEmpty())
						  .provide(just(l -> {
							  l.removeFirst();
							  return l;
						  }));

		ChainArbitrary<List<Integer>> chains =
			Chain.startWith(() -> (List<Integer>) new ArrayList<Integer>())
				 .withTransformation(addRandomIntToList)
				 .withTransformation(removeFirstElement)
				 .withMaxTransformations(13);

		GenRecorder recorder = new GenRecorder(new RandomGenSource("43"));

		Chain<List<Integer>> chain = chains.generator().generate(recorder);
		chain.forEachRemaining(ignore -> {});
		List<Integer> result1 = chain.current().get();
		// System.out.println(recorder.recording());

		chain = chains.generator().generate(RecordedSource.of(recorder.recording()));
		chain.forEachRemaining(ignore -> {});
		List<Integer> result2 = chain.current().get();

		assertThat(result1).isEqualTo(result2);
	}

	@Example
	void chainCanBeReplayed() {

		Transformation<List<Integer>> addRandomIntToList =
			ignore -> integers().between(0, 10)
								.map(i -> l -> {
									l.add(i);
									return l;
								});

		Transformation<List<Integer>> removeFirstElement =
			Transformation.<List<Integer>>when(last -> !last.isEmpty())
						  .provide(just(l -> {
							  l.removeFirst();
							  return l;
						  }));

		ChainArbitrary<List<Integer>> chains =
			Chain.startWith(() -> (List<Integer>) new ArrayList<Integer>())
				 .withTransformation(addRandomIntToList)
				 .withTransformation(removeFirstElement)
				 .withMaxTransformations(13);

		RandomGenSource source = new RandomGenSource("43");

		Chain<List<Integer>> chain = chains.generator().generate(source);
		List<List<Integer>> originalValues = collectAllValues(chain);
		Iterator<List<Integer>> chain1 = chain.replay();
		List<List<Integer>> values = new ArrayList<>();
		while (chain1.hasNext()) {
			chain1.hasNext(); // is idempotent
			values.add(chain1.next());
		}
		List<List<Integer>> replayedValues = values;

		assertThat(originalValues).isEqualTo(replayedValues);
	}

	private <T> List<T> collectAllValues(Iterator<T> chain) {
		List<T> values = new ArrayList<>();
		while (chain.hasNext()) {
			values.add(chain.next());
		}
		return values;
	}

	@Group
	@PropertyDefaults(tries = 10)
	class Shrinking {

		@Property
		void shrinkChainWithoutStateAccessToEnd(@ForAll long seed) {
			Arbitrary<Chain<Integer>> chains =
				Chain.startWith(() -> 0)
					 .withTransformation(ignore -> integers().between(0, 10).map(i -> t -> t + i))
					 .withMaxTransformations(5);

			Tryable falsifier = Tryable.from(params -> {
				Chain<Integer> chain = (Chain<Integer>) params.getFirst();
				chain.forEachRemaining(ignore -> {});
				return false;
			});

			Chain<Integer> shrunkChain = failAndShrink(seed, chains, falsifier);

			// System.out.println(shrunkChain.transformations().size());
			// System.out.println(shrunkChain.current());

			assertThat(shrunkChain.transformations()).hasSize(1);
			assertThat(shrunkChain.current()).hasValue(0);

			assertThat(collectAllValues(shrunkChain.replay()))
				.containsExactly(0, 0);
		}

		@Property
		void shrinkChainWithStateAccessToEnd(@ForAll long seed) {
			Arbitrary<Chain<Integer>> chains =
				Chain.startWith(() -> 0)
					 .withTransformation(
						 previous -> {
							 int min = Math.min(previous, 10);
							 return integers().between(min, 10).map(i -> Transformer.transform("+" + i, t -> t + i));
						 }
					 ).withMaxTransformations(5);

			Tryable falsifier = Tryable.from(params -> {
				Chain<Integer> chain = (Chain<Integer>) params.getFirst();
				chain.forEachRemaining(value -> {});
				return false;
			});

			Chain<Integer> shrunkChain = failAndShrink(seed, chains, falsifier);

			assertThat(shrunkChain.transformations()).hasSize(1);
			assertThat(collectAllValues(shrunkChain.replay())).containsExactly(0, 0);
		}

		@Property
		void shrinkAwayTransformersThatDontChangeState(@ForAll long seed) {
			Transformer<Integer> addOne = Transformer.transform("addOne", t1 -> t1 + 1);
			Transformer<Integer> doNothing = Transformer.transform("doNothing", t -> t);

			Arbitrary<Chain<Integer>> chains =
				Chain.startWith(() -> 1)
					 .withTransformation(ignore -> just(addOne))
					 .withTransformation(ignore -> just(doNothing))
					 .withMaxTransformations(20); // Size must be large enough to have at least a single addOne transformer

			Tryable falsifier = Tryable.from(params -> {
				Chain<Integer> chain = (Chain<Integer>) params.getFirst();
				int last = 1;
				while (chain.hasNext()) {
					last = chain.next();
				}
				return last <= 1;
			});

			Chain<Integer> shrunkChain = failAndShrink(seed, chains, falsifier);
			// System.out.println(shrunkChain);

			assertThat(shrunkChain.transformations()).hasSize(1);
			assertThat(collectAllValues(shrunkChain.replay())).containsExactly(1, 2);
		}

		@Property
		void fullyShrinkTransformersWithoutStateAccess(@ForAll long seed) {
			Arbitrary<Chain<Integer>> chains =
				Chain.startWith(() -> 0)
					 .withTransformation(ignore -> integers().between(1, 5).map(i -> Transformer.transform("add" + i, t -> t + i)))
					 .withMaxTransformations(10);

			Tryable falsifier = Tryable.from(params -> {
				Chain<Integer> chain = (Chain<Integer>) params.getFirst();
				int last = 1;
				while (chain.hasNext()) {
					last = chain.next();
				}
				return last < 3;
			});

			Chain<Integer> shrunkChain = failAndShrink(seed, chains, falsifier);

			// There are currently 2 possible "smallest" chains being shrunk to
			assertThat(collectAllValues(shrunkChain.replay())).isIn(
				Arrays.asList(0, 3),
				Arrays.asList(0, 1, 2, 3)
			);
		}

		@Property
		void shrinkChainWithStateAccess(@ForAll long seed) {
			Arbitrary<Chain<Integer>> chains =
				Chain.startWith(() -> 1)
					 .withTransformation(
						 previous -> {
							 int max = Math.max(previous, 2);
							 return integers().between(0, max)
											  .map(i -> Transformer.transform("+" + i, t -> t + i));
						 }
					 ).withMaxTransformations(10);

			Tryable falsifier = Tryable.from(params -> {
				Chain<Integer> chain = (Chain<Integer>) params.getFirst();
				int count = 0;
				int sum = 0;
				while (chain.hasNext()) {
					sum += chain.next();
					count++;
					if (count >= 5 && sum >= 5) {
						return false;
					}
				}
				return true;
			});

			Chain<Integer> shrunkChain = failAndShrink(seed, chains, falsifier);
			// System.out.println(shrunkChain.transformations());

			assertThat(shrunkChain.transformations()).hasSize(4);
			assertThat(collectAllValues(shrunkChain.replay())).containsExactly(1, 1, 1, 1, 1);
		}

		private static Chain<Integer> failAndShrink(long seed, Arbitrary<Chain<Integer>> chains, Tryable falsifier) {
			PropertyCase propertyCase = new PropertyCase(List.of(chains.generator()), falsifier);

			PropertyRunConfiguration configuration = PropertyRunConfiguration.randomized(Long.toString(seed), 100);
			PropertyRunResult result = propertyCase.run(configuration);
			assertThat(result.isFailed()).isTrue();

			FalsifiedSample smallestFalsifiedSample = result.falsifiedSamples().first();
			// FalsifiedSample smallestFalsifiedSample = result.falsifiedSamples().last();
			return (Chain<Integer>) smallestFalsifiedSample.values().getFirst();
		}
	}

}
