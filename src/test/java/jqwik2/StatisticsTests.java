package jqwik2;

import java.time.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.description.*;
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


		Statistics.Collector.C1<Integer> collector = Statistics.collector("numbers", Integer.class);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		var property = PropertyDescription.property().forAll(integers).check(i -> {
			collector.collect(i);
			return true;
		});
		PropertyValidator.forProperty(property).validate(strategy);

		// TODO: Check for reasonable values in collector
		// for (Integer value : collector.values()) {
		// 	System.out.println("Value " + value + " occurred " + collector.count(value) + " times");
		// }
	}

	@Group
	class ClassifyingCollectors {

		@Example
		void classifierAccept() {
			var classifier = new ClassifyingCollector<Integer>();
			classifier.addCase("Case 1", 40.0, anInt -> anInt >= 10);
			classifier.addCase("Case 2", 40.0, anInt -> anInt <= -10);
			classifier.addCase("Default", 5.0, anInt -> true);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource();

			while (true) {
				ClassifyingCollector.CoverageCheck result;
				synchronized (classifier) {result = classifier.checkCoverage();}
				if (!(result == ClassifyingCollector.CoverageCheck.UNSTABLE)) break;
				classifier.classify(integers.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.rejections());

			ClassifyingCollector.CoverageCheck result;
			synchronized (classifier) {result = classifier.checkCoverage();}
			assertThat(result).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
			assertThat(classifier.rejections()).isEmpty();
		}

		@Example
		void singleCaseAccept() {
			var classifier = new ClassifyingCollector<Integer>();
			classifier.addCase("Case 1", 40.0, anInt -> anInt >= 10);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource("42");

			while (true) {
				ClassifyingCollector.CoverageCheck result;
				synchronized (classifier) {result = classifier.checkCoverage();}
				if (!(result == ClassifyingCollector.CoverageCheck.UNSTABLE)) break;
				classifier.classify(integers.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.rejections());

			ClassifyingCollector.CoverageCheck result;
			synchronized (classifier) {result = classifier.checkCoverage();}
			assertThat(result).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
			assertThat(classifier.rejections()).isEmpty();
		}

		@Example
		void classifierAcceptUniformDie() {
			var classifier = new ClassifyingCollector<Integer>(new StatisticalError(0.01, 1e-6));

			// Actual rolling dice here does not seem to be very uniform
			classifier.addCase("1", 13.0, dieThrow -> dieThrow == 1);
			classifier.addCase("2", 13.0, dieThrow -> dieThrow == 2);
			classifier.addCase("3", 13.0, dieThrow -> dieThrow == 3);
			classifier.addCase("4", 13.0, dieThrow -> dieThrow == 4);
			classifier.addCase("5", 13.0, dieThrow -> dieThrow == 5);
			classifier.addCase("6", 13.0, dieThrow -> dieThrow == 6);

			Generator<Integer> die = BaseGenerators.choose(List.of(1, 2, 3, 4, 5, 6));
			GenSource source = new RandomGenSource();

			while (true) {
				ClassifyingCollector.CoverageCheck result;
				synchronized (classifier) {result = classifier.checkCoverage();}
				if (!(result == ClassifyingCollector.CoverageCheck.UNSTABLE)) break;
				classifier.classify(die.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.rejections());

			ClassifyingCollector.CoverageCheck result;
			synchronized (classifier) {result = classifier.checkCoverage();}
			assertThat(result).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
			assertThat(classifier.rejections()).isEmpty();
		}

		@Example
		void classifierReject() {
			var classifier = new ClassifyingCollector<Integer>();
			classifier.addCase("Case 1", 35.0, anInt-> anInt >= 10);
			classifier.addCase("Case 2", 35.0, anInt-> anInt <= -10);
			classifier.addCase("Default", 12.0,anInt -> true);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource();

			while (true) {
				ClassifyingCollector.CoverageCheck result;
				synchronized (classifier) {result = classifier.checkCoverage();}
				if (!(result == ClassifyingCollector.CoverageCheck.UNSTABLE)) break;
				classifier.classify(integers.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.rejections());

			ClassifyingCollector.CoverageCheck result;
			synchronized (classifier) {result = classifier.checkCoverage();}
			assertThat(result).isEqualTo(ClassifyingCollector.CoverageCheck.REJECT);
			assertThat(classifier.rejections()).hasSize(1);

			String rejection = classifier.rejections().iterator().next();
			// System.out.println(rejection);
			assertThat(rejection).startsWith("Coverage of case 'Default' expected to be at least 12.00% but was only ");
		}

		@Example
		void tightAccept() {
			var classifier = new ClassifyingCollector<Integer>(new StatisticalError(0.01, 1e-6));
			classifier.addCase("even", 49.5, anInt -> anInt % 2 == 0);
			classifier.addCase("odd", 49.5,  anInt -> anInt % 2 != 0);

			Generator<Integer> integers = BaseGenerators.integers(1, 100);
			GenSource source = new RandomGenSource();

			while (true) {
				ClassifyingCollector.CoverageCheck result;
				synchronized (classifier) {result = classifier.checkCoverage();}
				if (!(result == ClassifyingCollector.CoverageCheck.UNSTABLE)) break;
				classifier.classify(integers.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.rejections());

			ClassifyingCollector.CoverageCheck result;
			synchronized (classifier) {result = classifier.checkCoverage();}
			assertThat(result).isEqualTo(ClassifyingCollector.CoverageCheck.ACCEPT);
		}

		@Example
		void tightReject() {
			var classifier = new ClassifyingCollector<Integer>();
			classifier.addCase("even", 50.5, anInt -> anInt % 2 == 0);
			classifier.addCase("odd", 49.5,  anInt -> anInt % 2 != 0);

			Generator<Integer> integers = BaseGenerators.integers(1, 100);
			GenSource source = new RandomGenSource();

			while (true) {
				ClassifyingCollector.CoverageCheck result;
				synchronized (classifier) {result = classifier.checkCoverage();}
				if (!(result == ClassifyingCollector.CoverageCheck.UNSTABLE)) break;
				classifier.classify(integers.generate(source));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.rejections());

			ClassifyingCollector.CoverageCheck result;
			synchronized (classifier) {result = classifier.checkCoverage();}
			assertThat(result).isEqualTo(ClassifyingCollector.CoverageCheck.REJECT);
		}
	}
}

