package jqwik2.internal.statistics;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.statistics.*;
import org.opentest4j.*;

public class StatisticsChecker implements Statistics.Checker {
	private List<Hypothesis> hypotheses = new ArrayList<>();

	@Override
	public Statistics.Checker and(String label, Predicate<Double> nPredicate) {
		hypotheses.add(Hypothesis.claim(label, nPredicate));
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
			// TODO: Test hypotheses constantly to see if sufficient number of tests has been run
			if (counter.get() >= 1000) {
				return false;
			}
			return source.hasNext();
		}

		@Override
		public SampleSource next() {
			return source.next();
		}

		@Override
		public void guide(TryExecutionResult result, Sample sample) {
			counter.incrementAndGet();
		}

		@Override
		public PropertyRunResult overridePropertyResult(PropertyRunResult originalResult) {
			if (originalResult.isAborted() || originalResult.isFailed()) {
				return originalResult;
			}
			for (Hypothesis hypothesis : hypotheses) {
				if (!hypothesis.test(counter.get())) {
					var failureReason = new AssertionFailedError("Hypothesis [%s] failed".formatted(hypothesis.label()));
					return originalResult.withStatus(PropertyRunResult.Status.FAILED)
										 .withFailureReason(failureReason);
				}
			}
			return originalResult;
		}
	}
}
