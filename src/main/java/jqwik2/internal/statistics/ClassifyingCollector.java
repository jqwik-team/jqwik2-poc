package jqwik2.internal.statistics;

import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.internal.*;

public class ClassifyingCollector<C> {

	private final static Case<?> FALLTHROUGH_CASE = new Case<>("_", 0.0, ignore -> true);

	public record Case<C>(String label, double minPercentage, Predicate<C> condition) {
	}

	public enum CoverageCheck {
		ACCEPT, REJECT, UNSTABLE
	}

	private static final int MIN_TRIES_LOWER_BOUND = 100;

	static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.00", new DecimalFormatSymbols(Locale.US));

	private final List<ClassifyingCollector.Case<C>> cases = new ArrayList<>();

	private final Map<ClassifyingCollector.Case<C>, Integer> counts = new HashMap<>();
	private final Map<ClassifyingCollector.Case<C>, Double> sumOfPercentages = new HashMap<>();
	private final Map<ClassifyingCollector.Case<C>, Double> sumOfPercentageSquares = new HashMap<>();
	private final AtomicInteger total = new AtomicInteger(0);
	private int minTries = 0;

	private double alpha = 0.01;
	private double beta = 10e-6;
	private double lowerBound = Math.log10(beta / (1.0 - alpha));
	private double upperBound = Math.log10((1.0 - beta) / alpha);
	private double minLog = lowerBound / 10;
	private double maxLog = upperBound / 10;
	private final Map<ClassifyingCollector.Case<C>, Double> log10Alphas = new HashMap<>();
	private final Map<ClassifyingCollector.Case<C>, Integer> caseHitCounts = new HashMap<>();


	public ClassifyingCollector() {
		initializeCase(fallThroughCase());
		System.out.println("alpha=" + alpha);
		System.out.println("beta=" + beta);
		System.out.println("lowerBound=" + lowerBound);
		System.out.println("upperBound=" + upperBound);
		System.out.println("minLog=" + minLog);
		System.out.println("maxLog=" + maxLog);
	}

	public List<String> labels() {
		return cases.stream().map(Case::label).toList();
	}

	@SuppressWarnings("unchecked")
	private static <C> Case<C> fallThroughCase() {
		return (Case<C>) FALLTHROUGH_CASE;
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
		updateCaseCounts(args);
		updateSPRTValues();
	}

	private void updateSPRTValues() {
		// Parallel implementation of Sequential probability ratio test (SPRT)
		// System.out.printf("%ntotal=%d%n", total.get());
		for (Case<C> c : cases) {
			updateLog10Alpha(c);
		}
	}

	private void updateCaseCounts(C args) {
		for (Case<C> c : cases) {
			if (c.condition().test(args)) {
				classifyCase(c);
				return;
			}
		}
		classifyCase(fallThroughCase());
	}

	private void classifyCase(Case<C> c) {
		counts.put(c, counts.get(c) + 1);
		updateSums();
		updateSquares();
	}

	private void updateLog10Alpha(Case<C> c) {
		var caseHits = caseHitCounts.computeIfAbsent(c, ignore -> 0);

		var caseConditionHolds = isCaseConditionHit(c);
		if (caseConditionHolds) {
			caseHits += 1;
			caseHitCounts.put(c, caseHits);
		}

		var caseMisses = total.get() - caseHits;
		var caseHitRatio = caseMisses == 0 ? 0.0 : (double) caseHits / caseMisses;
		double nextLog10 = caseHits == 0 ? minLog
			: caseMisses == 0 ? maxLog
			: Math.log10(caseHitRatio);
		double log10Alpha = log10Alphas.getOrDefault(c, 0.0);
		log10Alpha += nextLog10;
		log10Alphas.put(c, log10Alpha);

		// System.out.printf("%s: counts=%s%n", c.label, counts.get(c));
		// System.out.printf("%s: caseConditionHolds=%s%n", c.label, caseConditionHolds);
		// System.out.printf("%s: caseHits=%s%n", c.label, caseHits);
		// System.out.printf("%s: caseMisses=%s%n", c.label, caseMisses);
		// System.out.printf("%s: caseHitRatio=%s%n", c.label, caseHitRatio);
		// System.out.printf("%s: nextLog10=%s%n", c.label, nextLog10);
		// System.out.printf("%s: log10Alpha=%s%n", c.label, log10Alpha);
		// if (log10Alpha < lowerBound) {
		// 	System.out.printf("%s: REJECT%n", c.label);
		// } else if (log10Alpha > upperBound) {
		// 	System.out.printf("%s: ACCEPT%n", c.label);
		// } else {
		// 	System.out.printf("%s: UNSTABLE%n", c.label);
		// }
	}

	private boolean isCaseConditionHit(Case<C> c) {
		return percentage(c) >= c.minPercentage();
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

	private double percentage(ClassifyingCollector.Case<C> c) {
		if (total.get() == 0) {
			return 0.0;
		}
		return Math.round(counts.get(c) / (double) total.get() * 10000.0) / 100.0;
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

	public Double minPercentage(String label) {
		var aCase = cases.stream()
						 .filter(c -> c.label().equals(label))
						 .findFirst();
		return aCase.map(c -> c.minPercentage).orElse(0.0);
	}

	public int count(String label) {
		var aCase = cases.stream()
						 .filter(c -> c.label().equals(label))
						 .findFirst();
		if (aCase.isEmpty()) {
			throw new IllegalArgumentException("No such case: " + label);
		}
		return counts.getOrDefault(aCase.get(), 0);
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

	private double deviation(ClassifyingCollector.Case<C> key) {
		var percentage = sumOfPercentages.get(key) / total.get();
		var percentageSquare = sumOfPercentageSquares.get(key) / total.get();
		return Math.sqrt(percentageSquare - percentage * percentage);
	}

	public synchronized ClassifyingCollector.CoverageCheck checkCoverage(double maxStandardDeviationFactor) {
		if (total.get() < minTries()) {
			return ClassifyingCollector.CoverageCheck.UNSTABLE;
		}

		var checks = cases.stream()
						  .map(c -> checkCoverage(c, maxStandardDeviationFactor))
						  .toList();

		if (checks.stream().anyMatch(c -> c == ClassifyingCollector.CoverageCheck.REJECT)) {
			return ClassifyingCollector.CoverageCheck.REJECT;
		}
		if (checks.stream().anyMatch(c -> c == ClassifyingCollector.CoverageCheck.UNSTABLE)) {
			return ClassifyingCollector.CoverageCheck.UNSTABLE;
		}
		return ClassifyingCollector.CoverageCheck.ACCEPT;
	}

	private int minTries() {
		if (minTries == 0) {
			int minFromAlpha = (int) Math.ceil(1.0 / alpha);
			int minFromBeta = (int) Math.ceil(1.0 / beta);
			minTries = Math.max(MIN_TRIES_LOWER_BOUND, Math.max(minFromAlpha, minFromBeta));
			// double minPercentage = cases.stream()
			// 							.mapToDouble(Case::minPercentage)
			// 							.filter(p -> p > 0.0)
			// 							.min()
			// 							.orElse(0.0);
			// if (minPercentage > 0.0) {
			// 	int averageTriesFor10Hits = (int) (100.0 / minPercentage) * 10;
			// 	minTries = Math.max(MIN_TRIES_LOWER_BOUND, averageTriesFor10Hits  * cases.size());
			// } else {
			// 	minTries = MIN_TRIES_LOWER_BOUND;
			// }
		}
		return minTries;
	}

	private ClassifyingCollector.CoverageCheck checkCoverage(ClassifyingCollector.Case<C> c, double maxStandardDeviationFactor) {
		var log10Alpha = log10Alphas.get(c);
		if (log10Alpha < lowerBound && !isCaseConditionHit(c)){
			return ClassifyingCollector.CoverageCheck.REJECT;
		} else if (log10Alpha > upperBound && isCaseConditionHit(c)) {
			return ClassifyingCollector.CoverageCheck.ACCEPT;
		}
		return ClassifyingCollector.CoverageCheck.UNSTABLE;
	}

	private ClassifyingCollector.CoverageCheck checkCoverage_OLD(ClassifyingCollector.Case<C> c, double maxStandardDeviationFactor) {
		var percentage = percentage(c);
		var minPercentage = c.minPercentage();
		var maxDeviation = deviation(c) * maxStandardDeviationFactor;
		if (percentage < minPercentage) {
			if ((minPercentage - percentage) <= maxDeviation) {
				return ClassifyingCollector.CoverageCheck.UNSTABLE;
			} else {
				return ClassifyingCollector.CoverageCheck.REJECT;
			}
		} else {
			if ((percentage - minPercentage) <= maxDeviation) {
				return ClassifyingCollector.CoverageCheck.UNSTABLE;
			}
		}
		return ClassifyingCollector.CoverageCheck.ACCEPT;
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

	private String rejectionDetail(Pair<ClassifyingCollector.Case<C>, Double> caseAndPercentage) {
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

	public boolean hasCoverageCheck() {
		return cases.stream().anyMatch(c -> c.minPercentage() > 0.0);
	}
}
