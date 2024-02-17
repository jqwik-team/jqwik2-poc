package jqwik2.internal.statistics;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.statistics.*;
import org.opentest4j.*;

public class StatisticsChecker implements Statistics.Checker {
	public static final int MIN_TRIES = 100;
	private List<Hypothesis> hypotheses = new ArrayList<>();

	@Override
	public Statistics.Checker and(String label, Predicate<Double> nPredicate) {
		hypotheses.add(new Hypothesis(label, nPredicate));
		return this;
	}

	@Override
	public GuidedGeneration guideWith(IterableSampleSource source) {
		return new GuidedStatisticsChecker(source);
	}

	/**
	 * First naive implementation: Run 1000 times and check if all hypotheses are true
	 */
	private class GuidedStatisticsChecker implements GuidedGeneration {

		private final Iterator<SampleSource> source;
		private final AtomicInteger counter = new AtomicInteger(0);

		public GuidedStatisticsChecker(IterableSampleSource source) {
			this.source = source.iterator();
		}

		@Override
		public boolean hasNext() {
			if (!source.hasNext()) {
				return false;
			}
			if (counter.get() < MIN_TRIES) {
				return true;
			}
			if (anyHypothesisRejected()) {
				return false;
			}
			if (allHypothesesAccepted()) {
				return false;
			}
			return true;
		}

		private boolean allHypothesesAccepted() {
			return hypotheses.stream().allMatch(h -> h.check(counter.get()) == Hypothesis.CheckResult.ACCEPT);
		}

		private boolean anyHypothesisRejected() {
			return hypotheses.stream().anyMatch(h -> h.check(counter.get()) == Hypothesis.CheckResult.REJECT);
		}

		@Override
		public SampleSource next() {
			return source.next();
		}

		@Override
		public void guide(TryExecutionResult result, Sample sample) {
			counter.incrementAndGet();
			hypotheses.forEach(h -> h.checkAndAdapt(counter.get()));
		}

		@Override
		public PropertyRunResult overridePropertyResult(PropertyRunResult originalResult) {
			if (originalResult.isAborted() || originalResult.isFailed()) {
				return originalResult;
			}
			for (Hypothesis hypothesis : hypotheses) {
				var hypothesisTest = hypothesis.test(counter.get());
				if (!hypothesisTest) {
					var failureReason = new AssertionFailedError("Hypothesis [%s] failed".formatted(hypothesis.label()));
					return originalResult.withStatus(PropertyRunResult.Status.FAILED)
										 .withFailureReason(failureReason);
				}
			}
			return originalResult;
		}
	}
}
