package jqwik2.internal.statistics;

import java.util.*;

import jqwik2.api.*;
import org.opentest4j.*;

public class StatisticalPropertySource implements IterableSampleSource {

	public static final String SATISFIED_TRIES = "satisfied";

	private final IterableSampleSource randomSource;
	private final double minSatisfiedPercentage;
	private final double maxStandardDeviationFactor;

	public StatisticalPropertySource(
		IterableSampleSource randomSource,
		double minSatisfiedPercentage,
		double maxStandardDeviationFactor
	) {
		this.randomSource = randomSource;
		this.minSatisfiedPercentage = minSatisfiedPercentage;
		this.maxStandardDeviationFactor = maxStandardDeviationFactor;
	}

	@Override
	public boolean stopWhenFalsified() {
		return false;
	}

	@Override
	public Iterator<SampleSource> iterator() {
		return new StatisticalPropertyIterator();
	}

	private class StatisticalPropertyIterator implements Iterator<SampleSource>, Guidance {

		private final Iterator<SampleSource> source = randomSource.iterator();
		private final Classifier<TryExecutionResult> classifier;
		private volatile boolean stopped = false;

		private StatisticalPropertyIterator() {
			classifier = new Classifier<>();
			classifier.addCase(
				SATISFIED_TRIES, minSatisfiedPercentage,
				tryResult -> tryResult.status() == TryExecutionResult.Status.SATISFIED
			);
		}

		@Override
		public boolean hasNext() {
			if (!source.hasNext() || stopped) {
				return false;
			}
			return isCoverageUnstable();
		}

		private boolean isCoverageUnstable() {
			return classifier.checkCoverage(maxStandardDeviationFactor) == Classifier.CoverageCheck.UNSTABLE;
		}

		@Override
		public SampleSource next() {
			return source.next();
		}

		@Override
		public void guide(TryExecutionResult result, Sample sample) {
			classifier.classify(result);
		}

		@Override
		public PropertyRunResult overridePropertyResult(PropertyRunResult originalResult) {
			if (originalResult.isAborted() || originalResult.isSuccessful()) {
				return originalResult;
			}
			var optionalRejection = classifier.rejections().stream().findFirst();
			return optionalRejection.map(rejectionDetail -> {
				String message = "Satisfaction percentage expected to be at least %s%% but was only %s%% (%d/%d)".formatted(
					Classifier.PERCENTAGE_FORMAT.format(minSatisfiedPercentage),
					Classifier.PERCENTAGE_FORMAT.format(classifier.percentage(SATISFIED_TRIES)),
					classifier.counts().get(SATISFIED_TRIES),
					classifier.total()
				);
				var failureReason = new AssertionFailedError(message);
				return originalResult.withStatus(PropertyRunResult.Status.FAILED)
									 .withFailureReason(failureReason);
			}).orElse(originalResult.withStatus(PropertyRunResult.Status.SUCCESSFUL));
		}

		@Override
		public void stop() {
			this.stopped = true;
		}
	}
}
