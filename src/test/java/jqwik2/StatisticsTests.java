package jqwik2;

import java.text.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.statistics.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

import net.jqwik.api.*;

import static jqwik2.api.PropertyRunStrategy.GenerationMode.*;
import static org.assertj.core.api.Assertions.*;

class StatisticsTests {

	@Example
	void collectSingleValue() {
		var property = new JqwikProperty()
						   .withGeneration(RANDOMIZED)
						   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.OFF)
						   .withMaxTries(1000)
						   .withMaxRuntime(Duration.ZERO);

		property.onFailed((r, e) -> {
			throw new RuntimeException("Property failed: " + r, e);
		});

		Statistics.Collector.C1<Integer> collector = Statistics.collector("numbers", Integer.class);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);
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
		var property = new JqwikProperty()
						   .withGeneration(RANDOMIZED)
						   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.OFF)
						   .withMaxTries(0)
						   .withMaxRuntime(Duration.ZERO);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		AtomicInteger countEven = new AtomicInteger(0);
		PropertyRunResult result = property.forAll(integers).verify(
			i -> {
				if (i % 2 == 0) {
					countEven.incrementAndGet();
				}
			},
			// TODO: API is wrong. The predicate should be i % 2.
			// check("blabla", i % 2 == 0, p -> p > 0.48)
			Statistics.check("Even number occurs at least 48%", n -> countEven.get() / n > 0.48)
		);
		System.out.println(countEven.get() + " of " + result.countChecks());
		// assertThat(result.isSuccessful()).isTrue();
	}

	@Example
	@Disabled
	void statisticalCheckFailed() {
		var property = new JqwikProperty()
						   .withGeneration(RANDOMIZED)
						   .withEdgeCases(PropertyRunStrategy.EdgeCasesMode.OFF)
						   .withMaxTries(0)
						   .withMaxRuntime(Duration.ZERO);

		IntegerArbitrary integers = Numbers.integers().between(0, 100);

		AtomicInteger countEven = new AtomicInteger(0);
		PropertyRunResult result = property.forAll(integers).verify(
			i -> {
				if (i % 2 == 0) {
					countEven.incrementAndGet();
				}
			},
			Statistics.check("Even number occurs at least 52%", n -> countEven.get() / n > 0.52)
		);

		System.out.println(countEven.get() + " of " + result.countChecks());
		System.out.println(result.failureReason().get().getMessage());
		assertThat(result.isFailed()).isTrue();
	}

	@Example
	void classificationExamples() {
		var property = new JqwikProperty();

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
	class Classifiers {
		@Example
		void classifierAccept() {
			var classifier = new Classifier();
			classifier.addCase("Case 1", 40.0, args -> (int) args.get(0) >= 10);
			classifier.addCase("Case 2", 40.0, args -> (int) args.get(0) <= -10);
			classifier.addCase("Default", 5.0, args -> true);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(1.0) == Classifier.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.checkCoverage(1.0));

			assertThat(classifier.checkCoverage(1.0)).isEqualTo(Classifier.CoverageCheck.ACCEPT);
			assertThat(classifier.rejections()).isEmpty();
		}

		@Example
		void classifierReject() {
			var classifier = new Classifier();
			classifier.addCase("Case 1", 40.0, args -> (int) args.get(0) >= 10);
			classifier.addCase("Case 2", 40.0, args -> (int) args.get(0) <= -10);
			classifier.addCase("Default", 12.0, args -> true);

			Generator<Integer> integers = BaseGenerators.integers(-100, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(1.0) == Classifier.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.counts());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.deviations());
			// System.out.println(classifier.checkCoverage(1.0));

			assertThat(classifier.checkCoverage(1.0)).isEqualTo(Classifier.CoverageCheck.REJECT);
			assertThat(classifier.rejections()).hasSize(1);

			String rejection = classifier.rejections().iterator().next();
			// System.out.println(rejection);
			assertThat(rejection).startsWith("Coverage of case 'Default' expected to be at least 12.0% but was only ");
		}

		@Example
		void tightAccept() {
			var classifier = new Classifier();
			classifier.addCase("even", 49.5, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("odd", 49.5, args -> (int) args.get(0) % 2 != 0);

			Generator<Integer> integers = BaseGenerators.integers(1, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(3.0) == Classifier.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			// System.out.println();
			// System.out.println(classifier.total());
			// System.out.println(classifier.percentages());
			// System.out.println(classifier.deviations());
			// System.out.println(classifier.checkCoverage(3.0));

			assertThat(classifier.checkCoverage(3.0)).isEqualTo(Classifier.CoverageCheck.ACCEPT);
		}

		@Example
		void tightReject() {
			var classifier = new Classifier();
			classifier.addCase("even", 50.5, args -> (int) args.get(0) % 2 == 0);
			classifier.addCase("odd", 49.5, args -> (int) args.get(0) % 2 != 0);

			Generator<Integer> integers = BaseGenerators.integers(1, 100);
			GenSource source = new RandomGenSource();

			while (classifier.checkCoverage(3.0) == Classifier.CoverageCheck.UNSTABLE) {
				classifier.classify(List.of(integers.generate(source)));
			}

			System.out.println();
			System.out.println(classifier.total());
			System.out.println(classifier.percentages());
			System.out.println(classifier.deviations());
			System.out.println(classifier.checkCoverage(3.0));

			assertThat(classifier.checkCoverage(3.0)).isEqualTo(Classifier.CoverageCheck.REJECT);
		}
	}
}

class Classifier {

	public static final int MIN_TRIES = 100;
	public static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.0###", new DecimalFormatSymbols(Locale.US));
	private final List<Case> cases = new ArrayList<>();
	private final Map<Case, Integer> counts = new HashMap<>();
	private final Map<Case, Double> sumOfPercentages = new HashMap<>();
	private final Map<Case, Double> sumOfPercentageSquares = new HashMap<>();
	private final AtomicInteger total = new AtomicInteger(0);

	void addCase(String label, double minPercentage, Predicate<List<Object>> condition) {
		var newCase = new Case(label, minPercentage, condition);
		cases.add(newCase);
		counts.put(newCase, 0);
		sumOfPercentages.put(newCase, 0.0);
		sumOfPercentageSquares.put(newCase, 0.0);
	}

	void classify(List<Object> args) {
		total.incrementAndGet();
		for (Case c : cases) {
			if (c.condition().test(args)) {
				counts.put(c, counts.get(c) + 1);
				updateSums();
				updateSquares();
				break;
			}
		}
	}

	private void updateSquares() {
		cases.forEach(c -> {
			var percentage = percentage(c);
			double sum = sumOfPercentageSquares.get(c);
			sumOfPercentageSquares.put(c, sum + percentage * percentage);
		});
	}

	private void updateSums() {
		cases.forEach(c -> {
			var percentage = percentage(c);
			double sum = sumOfPercentages.get(c);
			sumOfPercentages.put(c, sum + percentage);
		});
	}

	private double percentage(Case c) {
		if (total.get() == 0) {
			return 0.0;
		}
		return counts.get(c) / (double) total.get() * 100.0;
	}

	Map<String, Integer> counts() {
		return counts.entrySet().stream()
					 .collect(
						 Collectors.toMap(
							 e -> e.getKey().label(),
							 Map.Entry::getValue
						 )
					 );
	}

	Map<String, Double> percentages() {
		return counts.entrySet().stream()
					 .collect(
						 Collectors.toMap(
							 e -> e.getKey().label(),
							 e -> percentage(e.getKey())
						 )
					 );
	}

	Map<String, Double> deviations() {
		return counts.entrySet().stream()
					 .collect(
						 Collectors.toMap(
							 e -> e.getKey().label(),
							 e -> deviation(e.getKey())
						 )
					 );
	}

	private double deviation(Case key) {
		var percentage = sumOfPercentages.get(key) / total.get();
		var percentageSquare = sumOfPercentageSquares.get(key) / total.get();
		return Math.sqrt(percentageSquare - percentage * percentage);
	}

	enum CoverageCheck {
		ACCEPT, REJECT, UNSTABLE
	}

	CoverageCheck checkCoverage(double maxDeviationFactor) {
		if (total.get() < MIN_TRIES) {
			return CoverageCheck.UNSTABLE;
		}

		var checks = cases.stream()
						  .map(c -> checkCoverage(c, maxDeviationFactor))
						  .toList();

		if (checks.stream().anyMatch(c -> c == CoverageCheck.REJECT)) {
			return CoverageCheck.REJECT;
		}
		if (checks.stream().anyMatch(c -> c == CoverageCheck.UNSTABLE)) {
			return CoverageCheck.UNSTABLE;
		}
		return CoverageCheck.ACCEPT;
	}

	private CoverageCheck checkCoverage(Case c, double maxDeviationFactor) {
		var percentage = percentage(c);
		var minPercentage = c.minPercentage();
		var maxDeviation = deviation(c) * maxDeviationFactor;
		if (percentage < minPercentage) {
			if ((minPercentage - percentage) <= maxDeviation) {
				return CoverageCheck.UNSTABLE;
			} else {
				return CoverageCheck.REJECT;
			}
		} else {
			if ((percentage - minPercentage) <= maxDeviation) {
				return CoverageCheck.UNSTABLE;
			}
		}
		return CoverageCheck.ACCEPT;
	}

	int total() {
		return total.get();
	}

	Set<String> rejections() {
		return cases.stream()
					.map(c -> Pair.of(c, percentage(c)))
					.filter(p -> p.second() <= p.first().minPercentage())
					.map(p -> rejectionMessage(p))
					.collect(Collectors.toSet());
	}

	private String rejectionMessage(Pair<Case, Double> caseAndPercentage) {
		var aCase = caseAndPercentage.first();
		var percentage = caseAndPercentage.second();
		return "Coverage of case '%s' expected to be at least %s%% but was only %s%% (%d/%d)".formatted(
			aCase.label(),
			PERCENTAGE_FORMAT.format(aCase.minPercentage()),
			PERCENTAGE_FORMAT.format(percentage),
			counts.get(aCase),
			total.get()
		);
	}
}

record Case(String label, double minPercentage, Predicate<List<Object>> condition) {
}
