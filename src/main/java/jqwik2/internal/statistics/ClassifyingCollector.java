package jqwik2.internal.statistics;

import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.statistics.*;
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
	private final AtomicInteger total = new AtomicInteger(0);
	private final Map<ClassifyingCollector.Case<C>, Double> logAlphaSums = new HashMap<>();
	private final Map<ClassifyingCollector.Case<C>, Integer> caseHitCounts = new HashMap<>();

	private final double alpha;
	private final double beta;
	private final double lowerBound;
	private final double upperBound;
	private final double minLog;
	private final double maxLog;

	private int minTries = 0;

	public ClassifyingCollector() {
		this(new StatisticalError(0.01, 1e-3));
	}

	public ClassifyingCollector(StatisticalError allowedError) {

		// Initialize SPRT parameters
		this.alpha = allowedError.alpha();
		this.beta = allowedError.beta();
		this.lowerBound = Math.log10(beta / (1.0 - alpha));
		this.upperBound = Math.log10((1.0 - beta) / alpha);
		this.minLog = lowerBound / 10;
		this.maxLog = upperBound / 10;

		initializeCase(fallThroughCase());
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
		logAlphaSums.put(newCase, 0.0);
		caseHitCounts.put(newCase, 0);
	}

	public synchronized void classify(C args) {
		total.incrementAndGet();
		updateCaseCounts(args);
		updateSPRTValues();
	}

	private void updateSPRTValues() {
		for (Case<C> c : cases) {
			updateSPRT(c);
		}
	}

	private void updateCaseCounts(C args) {
		for (Case<C> c : cases) {
			if (c.condition().test(args)) {
				updateCounts(c);
				return;
			}
		}
		updateCounts(fallThroughCase());
	}

	private void updateCounts(Case<C> c) {
		counts.put(c, counts.get(c) + 1);
	}

	/**
	 * This method contains the SPRT magic, as described e.g. in https://en.wikipedia.org/wiki/Sequential_probability_ratio_test
	 * Hypothesis H0: The case condition is not met
	 * Hypothesis H1: The case condition is met, ie the percentage of the case is at least minPercentage
	 */
	private void updateSPRT(Case<C> c) {
		int caseHits = caseHitCounts.get(c);

		var isCaseConditionHit = isCaseConditionHit(c);
		if (isCaseConditionHit) {
			caseHits += 1;
			caseHitCounts.put(c, caseHits);
		}

		var caseMisses = total() - caseHits;

		// logAlpha calculation requires special handling when there are no hits or misses
		var caseHitRatio = caseMisses == 0 ? 0.0 : (double) caseHits / caseMisses;
		double logAlpha = caseHits == 0 ? minLog
			: caseMisses == 0 ? maxLog
			: Math.log10(caseHitRatio);

		double logAlphaSum = logAlphaSums.get(c) + logAlpha;
		logAlphaSums.put(c, logAlphaSum);
	}

	private boolean isCaseConditionHit(Case<C> c) {
		return percentage(c) >= c.minPercentage();
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

	public synchronized ClassifyingCollector.CoverageCheck checkCoverage() {
		if (total.get() < minTries()) {
			return ClassifyingCollector.CoverageCheck.UNSTABLE;
		}

		var checks = cases.stream()
						  .map(this::checkCoverage)
						  .toList();

		if (checks.stream().anyMatch(c -> c == ClassifyingCollector.CoverageCheck.REJECT)) {
			return ClassifyingCollector.CoverageCheck.REJECT;
		}
		if (checks.stream().anyMatch(c -> c == ClassifyingCollector.CoverageCheck.UNSTABLE)) {
			return ClassifyingCollector.CoverageCheck.UNSTABLE;
		}
		return ClassifyingCollector.CoverageCheck.ACCEPT;
	}

	/**
	 * The minimum number of tries needed to make a decision.
	 * This depends on alpha and beta and an absolute minimum of MIN_TRIES_LOWER_BOUND.
	 *
	 * If the minimum is too low the statistical test is not reliable.
	 */
	private int minTries() {
		if (minTries == 0) {
			int minFromAlpha = (int) Math.ceil(1.0 / alpha);
			int minFromBeta = (int) Math.ceil(1.0 / beta);
			minTries = Math.max(MIN_TRIES_LOWER_BOUND, Math.max(minFromAlpha, minFromBeta));
		}
		return minTries;
	}

	private ClassifyingCollector.CoverageCheck checkCoverage(ClassifyingCollector.Case<C> c) {
		var log10Alpha = logAlphaSums.get(c);
		if (log10Alpha < lowerBound && !isCaseConditionHit(c)){
			return ClassifyingCollector.CoverageCheck.REJECT;
		} else if (log10Alpha > upperBound && isCaseConditionHit(c)) {
			return ClassifyingCollector.CoverageCheck.ACCEPT;
		}
		return ClassifyingCollector.CoverageCheck.UNSTABLE;
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
