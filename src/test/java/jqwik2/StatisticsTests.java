package jqwik2;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.statistics.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.statistics.*;

import net.jqwik.api.*;

import static jqwik2.api.validation.PropertyValidationStrategy.GenerationMode.*;
import static org.assertj.core.api.Assertions.*;

class StatisticsTests {

	@Example
	void collectSingleValue() {
		var strategy = PropertyValidationStrategy.builder()
												 .withGeneration(RANDOMIZED)
												 .withEdgeCases(PropertyValidationStrategy.EdgeCasesMode.OFF)
												 .withMaxTries(1000)
												 .withMaxRuntime(Duration.ZERO)
												 .build();

		var property = new OLD_JqwikProperty(strategy);

		Statistics.Collector.C1<Integer> collector = Statistics.collector("numbers", Integer.class);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		// TODO: Replace with property validator
		PropertyRunResult result = property.forAll(integers).check(i -> {
			collector.collect(i);
			return true;
		});

		// TODO: Check for reasonable values in collector
		// for (Integer value : collector.values()) {
		// 	System.out.println("Value " + value + " occurred " + collector.count(value) + " times");
		// }
	}

	@Example
	@Disabled
	void statisticalCheckSuccessful() {
		var strategy = PropertyValidationStrategy.builder()
												 .withGeneration(RANDOMIZED)
												 .withEdgeCases(PropertyValidationStrategy.EdgeCasesMode.OFF)
												 .withMaxTries(0)
												 .withMaxRuntime(Duration.ZERO)
												 .build();
		var property = new OLD_JqwikProperty(strategy);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		AtomicInteger countEven = new AtomicInteger(0);
		// PropertyRunResult result = property.forAll(integers).verify(
		// 	i -> {
		// 		if (i % 2 == 0) {
		// 			countEven.incrementAndGet();
		// 		}
		// 	},
		// 	// TODO: API is wrong. The predicate should be i % 2.
		// 	// check("blabla", i % 2 == 0, p -> p > 0.48)
		// 	Statistics.check("Even number occurs at least 48%", n -> countEven.get() / n > 0.48)
		// );
		// System.out.println(countEven.get() + " of " + result.countChecks());
		// assertThat(result.isSuccessful()).isTrue();
	}

	@Example
	@Disabled
	void statisticalCheckFailed() {
		var strategy = PropertyValidationStrategy.builder()
												 .withGeneration(RANDOMIZED)
												 .withEdgeCases(PropertyValidationStrategy.EdgeCasesMode.OFF)
												 .withMaxTries(0)
												 .withMaxRuntime(Duration.ZERO)
												 .build();

		var property = new OLD_JqwikProperty(strategy);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		AtomicInteger countEven = new AtomicInteger(0);
		// PropertyRunResult result = property.forAll(integers).verify(
		// 	i -> {
		// 		if (i % 2 == 0) {
		// 			countEven.incrementAndGet();
		// 		}
		// 	},
		// 	Statistics.check("Even number occurs at least 52%", n -> countEven.get() / n > 0.52)
		// );

		// System.out.println(countEven.get() + " of " + result.countChecks());
		// System.out.println(result.failureReason().get().getMessage());
		// assertThat(result.isFailed()).isTrue();
	}

	@Example
	void classificationExamples() {
		var property = new OLD_JqwikProperty();

		// property.forAll(Numbers.integers()).classify(
		// 	caseOf("Even number", percentage(40), i -> i % 2 == 0).verify(i -> i % 2 == 0),
		// 	caseOf("Odd number", percentage(40), i -> i % 2 != 0).verify(i -> i % 2 != 0)
		// );

		// int i = 100;
		// classify(
		// 	caseOf("Even number", i -> i % 2 == 0, () -> {
		//
		// 	}),
		// )
	}

	@Group
	class ClassifyingCollectors {

		@Example
		void classifierAccept() {
			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("Case 1", 40.0, args -> (int) args.get(0) >= 10);
			classifier.addCase("Case 2", 40.0, args -> (int) args.get(0) <= -10);
			classifier.addCase("Default", 5.0, args -> true);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(1.0) == ClassifyingCollector.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.checkCoverage(1.0));

			assertThat(classifier.checkCoverage(1.0)).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
			assertThat(classifier.rejections()).isEmpty();
		}

		@Example
		void singleCaseAccept() {
			var classifier = new ClassifyingCollector<Integer>();
			classifier.addCase("Case 1", 40.0, anInt -> anInt >= 10);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource("42");

			while (classifier.checkCoverage(1.0) == ClassifyingCollector.CoverageCheck.UNSTABLE) {
				classifier.classify(integers.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.checkCoverage(1.0));

			assertThat(classifier.checkCoverage(1.0)).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
			assertThat(classifier.rejections()).isEmpty();
		}

		@Example
		void classifierAcceptUniformDie() {
			var classifier = new ClassifyingCollector<Integer>();
			classifier.addCase("1", 14.0, dieThrow -> dieThrow == 1);
			classifier.addCase("2", 14.0, dieThrow -> dieThrow == 2);
			classifier.addCase("3", 14.0, dieThrow -> dieThrow == 3);
			classifier.addCase("4", 14.0, dieThrow -> dieThrow == 4);
			classifier.addCase("5", 14.0, dieThrow -> dieThrow == 5);
			classifier.addCase("6", 14.0, dieThrow -> dieThrow == 6);

			Generator<Integer> die = BaseGenerators.choose(List.of(1, 2, 3, 4, 5, 6));
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(2.0) == ClassifyingCollector.CoverageCheck.UNSTABLE) {
				classifier.classify(die.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.checkCoverage(1.0));
			// System.out.println(classifier.rejections());

			assertThat(classifier.checkCoverage(2.0)).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
			assertThat(classifier.rejections()).isEmpty();
		}

		@Example
		void classifierReject() {
			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("Case 1", 35.0, args -> (int) args.get(0) >= 10);
			classifier.addCase("Case 2", 35.0, args -> (int) args.get(0) <= -10);
			classifier.addCase("Default", 12.0, args -> true);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(1.0) == ClassifyingCollector.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.counts());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.deviations());
			// System.out.println(classifier.checkCoverage(1.0));

			assertThat(classifier.checkCoverage(1.0)).isEqualTo(ClassifyingCollector.CoverageCheck.REJECT);
			assertThat(classifier.rejections()).hasSize(1);

			String rejection = classifier.rejections().iterator().next();
			// System.out.println(rejection);
			assertThat(rejection).startsWith("Coverage of case 'Default' expected to be at least 12.0% but was only ");
		}

		@Example
		void tightAccept() {
			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("even", 49.5, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("odd", 49.5, args -> (int) args.get(0) % 2 != 0);

			Generator<Integer> integers = BaseGenerators.integers(1, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(3.0) == ClassifyingCollector.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.deviations());
			// System.out.println(classifier.checkCoverage(3.0));

			assertThat(classifier.checkCoverage(3.0)).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
		}

		@Example
		void tightReject() {
			var classifier = new ClassifyingCollector<List<Object>>();
			classifier.addCase("even", 50.5, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("odd", 49.5, args -> (int) args.get(0) % 2 != 0);

			Generator<Integer> integers = BaseGenerators.integers(1, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(3.0) == ClassifyingCollector.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.deviations());
			// System.out.println(classifier.checkCoverage(3.0));

			assertThat(classifier.checkCoverage(3.0)).isEqualTo(ClassifyingCollector.CoverageCheck.REJECT);
		}
	}
}

