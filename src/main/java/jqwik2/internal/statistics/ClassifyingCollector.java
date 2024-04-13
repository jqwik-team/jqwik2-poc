package jqwik2.internal.statistics;

import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.statistics.*;
import jqwik2.internal.*;

/**
 * This class is used to collect statistics about the distribution of generated values
 * and to check if the percentage of hit cases is beyond a minimum probability.
 *
 * It uses the Sequential Probability Ratio Test (SPRT) to check if the percentage of hit cases
 * is beyond a minimum probability.
 * See e.g. https://en.wikipedia.org/wiki/Sequential_probability_ratio_test
 * or http://www.ist.tugraz.at/aichernig/publications/papers/icst17_smc.pdf
 */
public class ClassifyingCollector<C> {

	private final static Case<?> FALLTHROUGH_CASE = new Case<>("_", 0.0, ignore -> true);

	public record Case<C>(String label, double minPercentage, Predicate<C> condition) {
	}

	public enum CoverageCheck {
		ACCEPT, REJECT, UNSTABLE
	}

	static final DecimalFormat PERCENTAGE_FORMAT = new DecimalFormat("#.00", new DecimalFormatSymbols(Locale.US));

	private final List<ClassifyingCollector.Case<C>> cases = new ArrayList<>();
	private final Map<ClassifyingCollector.Case<C>, Integer> counts = new HashMap<>();
	private final AtomicInteger total = new AtomicInteger(0);
	private final Map<ClassifyingCollector.Case<C>, Double> logAlphaSums = new HashMap<>();

	private final double rejectH1Bound;
	private final double acceptH1Bound;

	public ClassifyingCollector() {
		this(JqwikDefaults.defaultAllowedStatisticalError());
	}

	public ClassifyingCollector(StatisticalError allowedError) {
		// Initialize SPRT accept / reject bounds
		this.rejectH1Bound = Math.log(allowedError.beta() / (1.0 - allowedError.alpha()));
		this.acceptH1Bound = Math.log((1.0 - allowedError.beta()) / allowedError.alpha());

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
	}

	public synchronized void classify(C args) {
		total.incrementAndGet();
		Case<C> hitCase = findHitCase(args);
		updateCounts(hitCase);
		updateSPRTValues(hitCase);
	}

	private Case<C> findHitCase(C args) {
		for (Case<C> c : cases) {
			if (c.condition().test(args)) {
				return c;
			}
		}
		return fallThroughCase();
	}

	private void updateSPRTValues(Case<C> hitCase) {
		for (Case<C> c : cases) {
			if (c == hitCase)
				updateSPRT(c, true);
			else
				updateSPRT(c, false);
		}
	}

	private void updateCounts(Case<C> c) {
		counts.put(c, counts.get(c) + 1);
	}

	/**
	 * This method contains the SPRT magic
	 * Hypothesis H0: The case is hit with minPercentage - hypothesisDelta
	 * Hypothesis H1: The case is hit with minPercentage + hypothesisDelta
	 */
	private void updateSPRT(Case<C> c, boolean caseHit) {

		// Most of these values could be cached per case since they never change
		var hypothesisDelta = hypothesisDelta(c.minPercentage());
		double h0HitProbability = (c.minPercentage() - hypothesisDelta) / 100.0;
		double h1HitProbability = (c.minPercentage() + hypothesisDelta) / 100.0;

		var caseHitRatio = caseHit
							   ? h1HitProbability / h0HitProbability
							   : (1 - h1HitProbability) / (1 - h0HitProbability);
		double logAlpha = Math.log(caseHitRatio);

		double logAlphaSum = logAlphaSums.get(c) + logAlpha;
		logAlphaSums.put(c, logAlphaSum);
	}

	/**
	 * The hypothesisDelta is a function of the minPercentage.
	 * It is chosen such that the SPRT is most sensitive at the edges of the minPercentage range.
	 */
	private static double hypothesisDelta(double minPercentage) {
		if (minPercentage < 1.5) {
			return minPercentage / 2;
		}
		if (minPercentage > 98.5) {
			return (100 - minPercentage) / 2;
		}
		return 1.0;
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

	public synchronized CoverageCheck checkCoverage() {
		var checks = cases.stream()
						  .map(this::checkCoverage)
						  .toList();

		if (checks.stream().anyMatch(c -> c == CoverageCheck.REJECT)) {
			return CoverageCheck.REJECT;
		}
		if (checks.stream().anyMatch(c -> c == CoverageCheck.UNSTABLE)) {
			return CoverageCheck.UNSTABLE;
		}
		return CoverageCheck.ACCEPT;
	}

	private CoverageCheck checkCoverage(ClassifyingCollector.Case<C> c) {
		if (c.minPercentage() <= 0.0) {
			return CoverageCheck.ACCEPT;
		}
		var log10Alpha = logAlphaSums.get(c);
		if (log10Alpha < rejectH1Bound && !isCaseConditionHit(c)) {
			return CoverageCheck.REJECT;
		} else if (log10Alpha > acceptH1Bound && isCaseConditionHit(c)) {
			return CoverageCheck.ACCEPT;
		}
		return CoverageCheck.UNSTABLE;
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
