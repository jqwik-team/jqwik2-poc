package jqwik2.internal.statistics;

import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.internal.*;

public class Classifier<C> {

	private final static Case<?> DEFAULT_CASE = new Case<>("_", 0.0, ignore -> true);

	public record Case<C>(String label, double minPercentage, Predicate<C> condition) {
	}

	public enum CoverageCheck {
		ACCEPT, REJECT, UNSTABLE
	}

	private static final int MIN_TRIES = 100;
	static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.0###", new DecimalFormatSymbols(Locale.US));

	private final List<Classifier.Case<C>> cases = new ArrayList<>();
	private final Map<Classifier.Case<C>, Integer> counts = new HashMap<>();
	private final Map<Classifier.Case<C>, Double> sumOfPercentages = new HashMap<>();
	private final Map<Classifier.Case<C>, Double> sumOfPercentageSquares = new HashMap<>();
	private final AtomicInteger total = new AtomicInteger(0);

	public Classifier() {
		initializeCase(defaultCase());
	}

	@SuppressWarnings("unchecked")
	private static <C> Case<C> defaultCase() {
		return (Case<C>) DEFAULT_CASE;
	}

	public void addCase(String label, double minPercentage, Predicate<C> condition) {
		var newCase = new Case<C>(label, minPercentage, condition);
		cases.add(newCase);
		initializeCase(newCase);
	}

	private void initializeCase(Case<C> newCase) {
		counts.put(newCase, 0);
		sumOfPercentages.put(newCase, 0.0);
		sumOfPercentageSquares.put(newCase, 0.0);
	}

	public synchronized void classify(C args) {
		total.incrementAndGet();
		for (Classifier.Case<C> c : cases) {
			if (c.condition().test(args)) {
				classifyCase(c);
				return;
			}
		}
		classifyCase(defaultCase());
	}

	private void classifyCase(Case<C> c) {
		counts.put(c, counts.get(c) + 1);
		updateSums();
		updateSquares();
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

	private double percentage(Classifier.Case<C> c) {
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

	public Double percentage(String label) {
		var aCase = cases.stream()
						 .filter(c -> c.label().equals(label))
						 .findFirst();
		return aCase.map(this::percentage).orElse(0.0);
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

	private double deviation(Classifier.Case<C> key) {
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

	private Classifier.CoverageCheck checkCoverage(Classifier.Case<C> c, double maxStandardDeviationFactor) {
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
					.map(this::rejectionDetail)
					.collect(Collectors.toSet());
	}

	private String rejectionDetail(Pair<Classifier.Case<C>, Double> caseAndPercentage) {
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
