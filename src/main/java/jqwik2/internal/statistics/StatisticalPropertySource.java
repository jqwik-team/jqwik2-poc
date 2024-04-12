package jqwik2.internal.statistics;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.statistics.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import org.opentest4j.*;

public class StatisticalPropertySource implements IterableSampleSource {

	public static final String SATISFIED_TRIES = "satisfied";

	private final IterableSampleSource randomSource;
	private final double minSatisfiedPercentage;
	private final StatisticalError allowedError;

	public StatisticalPropertySource(
		IterableSampleSource randomSource,
		double minSatisfiedPercentage,
		StatisticalError allowedError
	) {
		this.randomSource = randomSource;
		this.minSatisfiedPercentage = minSatisfiedPercentage;
		this.allowedError = allowedError;
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
		private final ClassifyingCollector<TryExecutionResult> classifier;
		private volatile boolean stopped = false;

		private StatisticalPropertyIterator() {
			classifier = new ClassifyingCollector<>(allowedError);
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
			return classifier.checkCoverage() == ClassifyingCollector.CoverageCheck.UNSTABLE;
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
		public Optional<Pair<PropertyValidationStatus, Throwable>> overrideValidationStatus(PropertyValidationStatus status) {
			if (status.isAborted() || status.isSuccessful()) {
				return Optional.empty();
			}
			var optionalRejection = classifier.rejections().stream().findFirst();
			if (optionalRejection.isPresent()) {
				String message = "Satisfaction percentage expected to be at least %s%% but was only %s%% (%d/%d)".formatted(
					ClassifyingCollector.PERCENTAGE_FORMAT.format(minSatisfiedPercentage),
					ClassifyingCollector.PERCENTAGE_FORMAT.format(classifier.percentage(SATISFIED_TRIES)),
					classifier.counts().get(SATISFIED_TRIES),
					classifier.total()
				);
				var failureReason = new AssertionFailedError(message);
				return Optional.of(Pair.of(PropertyValidationStatus.FAILED, failureReason));
			} else {
				return Optional.of(Pair.of(PropertyValidationStatus.SUCCESSFUL, null));
			}
		}

		@Override
		public void stop() {
			this.stopped = true;
		}
	}
}
