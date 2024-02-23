package jqwik2.internal.statistics;

import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.internal.*;

public class Classifier {

	public record Case(String label, double minPercentage, Predicate<List<Object>> condition) {
	}

	public enum CoverageCheck {
		ACCEPT, REJECT, UNSTABLE
	}

	private static final int MIN_TRIES = 100;
	private static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.0###", new DecimalFormatSymbols(Locale.US));

	private final List<Classifier.Case> cases = new ArrayList<>();
	private final Map<Classifier.Case, Integer> counts = new HashMap<>();
	private final Map<Classifier.Case, Double> sumOfPercentages = new HashMap<>();
	private final Map<Classifier.Case, Double> sumOfPercentageSquares = new HashMap<>();
	private final AtomicInteger total = new AtomicInteger(0);

	public void addCase(String label, double minPercentage, Predicate<List<Object>> condition) {
		var newCase = new Classifier.Case(label, minPercentage, condition);
		cases.add(newCase);
		counts.put(newCase, 0);
		sumOfPercentages.put(newCase, 0.0);
		sumOfPercentageSquares.put(newCase, 0.0);
	}

	public synchronized void classify(List<Object> args) {
		total.incrementAndGet();
		for (Classifier.Case c : cases) {
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

	private double percentage(Classifier.Case c) {
		if (total.get() == 0) {
			return 0.0;
		}
		return counts.get(c) / (double) total.get() * 100.0;
	}

	public Map<String, Integer> counts() {
		return counts.entrySet().stream()
					 .collect(
						 Collectors.toMap(
							 e -> e.getKey().label(),
							 Map.Entry::getValue
						 )
					 );
	}

	public Map<String, Double> percentages() {
		return counts.entrySet().stream()
					 .collect(
						 Collectors.toMap(
							 e -> e.getKey().label(),
							 e -> percentage(e.getKey())
						 )
					 );
	}

	public Map<String, Double> deviations() {
		return counts.entrySet().stream()
					 .collect(
						 Collectors.toMap(
							 e -> e.getKey().label(),
							 e -> deviation(e.getKey())
						 )
					 );
	}

	private double deviation(Classifier.Case key) {
		var percentage = sumOfPercentages.get(key) / total.get();
		var percentageSquare = sumOfPercentageSquares.get(key) / total.get();
		return Math.sqrt(percentageSquare - percentage * percentage);
	}

	public synchronized Classifier.CoverageCheck checkCoverage(double maxStandardDeviationFactor) {
		if (total.get() < MIN_TRIES) {
			return Classifier.CoverageCheck.UNSTABLE;
		}

		var checks = cases.stream()
						  .map(c -> checkCoverage(c, maxStandardDeviationFactor))
						  .toList();

		if (checks.stream().anyMatch(c -> c == Classifier.CoverageCheck.REJECT)) {
			return Classifier.CoverageCheck.REJECT;
		}
		if (checks.stream().anyMatch(c -> c == Classifier.CoverageCheck.UNSTABLE)) {
			return Classifier.CoverageCheck.UNSTABLE;
		}
		return Classifier.CoverageCheck.ACCEPT;
	}

	private Classifier.CoverageCheck checkCoverage(Classifier.Case c, double maxStandardDeviationFactor) {
		var percentage = percentage(c);
		var minPercentage = c.minPercentage();
		var maxDeviation = deviation(c) * maxStandardDeviationFactor;
		if (percentage < minPercentage) {
			if ((minPercentage - percentage) <= maxDeviation) {
				return Classifier.CoverageCheck.UNSTABLE;
			} else {
				return Classifier.CoverageCheck.REJECT;
			}
		} else {
			if ((percentage - minPercentage) <= maxDeviation) {
				return Classifier.CoverageCheck.UNSTABLE;
			}
		}
		return Classifier.CoverageCheck.ACCEPT;
	}

	public int total() {
		return total.get();
	}

	public Set<String> rejections() {
		return cases.stream()
					.map(c -> Pair.of(c, percentage(c)))
					.filter(p -> p.second() <= p.first().minPercentage())
					.map(p -> rejectionDetail(p))
					.collect(Collectors.toSet());
	}

	private String rejectionDetail(Pair<Classifier.Case, Double> caseAndPercentage) {
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
