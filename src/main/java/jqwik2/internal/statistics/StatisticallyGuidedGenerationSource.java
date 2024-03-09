package jqwik2.internal.statistics;

import java.util.*;

import jqwik2.api.*;
import org.opentest4j.*;

public class StatisticallyGuidedGenerationSource implements IterableSampleSource {

	private final IterableSampleSource randomSource;
	private final Set<ClassifyingCollector<List<Object>>> classifiers;
	private final double maxStandardDeviationFactor;

	public StatisticallyGuidedGenerationSource(
		IterableSampleSource randomSource,
		Set<ClassifyingCollector<List<Object>>> classifiers,
		double maxStandardDeviationFactor
	) {
		this.randomSource = randomSource;
		this.classifiers = classifiers;
		this.maxStandardDeviationFactor = maxStandardDeviationFactor;
	}

	@Override
	public Iterator<SampleSource> iterator() {
		return new ClassifiedIterator();
	}

	private class ClassifiedIterator implements Iterator<SampleSource>, Guidance {

		private final Iterator<SampleSource> source = randomSource.iterator();
		private volatile boolean stopped = false;

		@Override
		public boolean hasNext() {
			if (!source.hasNext() || stopped) {
				return false;
			}
			return isCoverageUnstable();
		}

		private boolean isCoverageUnstable() {
			for (var classifier : classifiers) {
				if (classifier.checkCoverage(maxStandardDeviationFactor) == ClassifyingCollector.CoverageCheck.UNSTABLE) {
					return true;
				}
			}
			return false;
		}

		private Optional<String> rejection() {
			for (var classifier : classifiers) {
				var rejections = classifier.rejections();
				if (rejections.isEmpty()) {
					continue;
				}
				return Optional.of(rejections.iterator().next());
			}
			return Optional.empty();
		}

		@Override
		public SampleSource next() {
			return source.next();
		}

		@Override
		public void guide(TryExecutionResult result, Sample sample) {
			// The statistical classification is done in the call to classifier.classify() in the tryable
		}

		@Override
		public PropertyRunResult overridePropertyResult(PropertyRunResult originalResult) {
			if (originalResult.isAborted() || originalResult.isFailed()) {
				return originalResult;
			}
			var optionalRejection = rejection();
			return optionalRejection.map(rejectionDetail -> {
				var failureReason = new AssertionFailedError(rejectionDetail);
				return originalResult.withStatus(PropertyRunResult.Status.FAILED)
									 .withFailureReason(failureReason);
			}).orElse(originalResult);
		}

		@Override
		public void stop() {
			this.stopped = true;
		}
	}
}
