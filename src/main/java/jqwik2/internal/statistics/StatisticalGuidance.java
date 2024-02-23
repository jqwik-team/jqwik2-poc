package jqwik2.internal.statistics;

import java.util.*;

import jqwik2.api.*;
import org.opentest4j.*;

public class StatisticalGuidance implements GuidedGeneration {

	private final Classifier classifier;
	private final double maxStandardDeviationFactor;
	private final Iterator<SampleSource> source;
	private volatile boolean stopped = false;

	public StatisticalGuidance(Classifier classifier, double maxStandardDeviationFactor, IterableSampleSource randomSource) {
		this.classifier = classifier;
		this.maxStandardDeviationFactor = maxStandardDeviationFactor;
		this.source = randomSource.iterator();
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
		// The statistical classification is done in the call to classifier.classify() in the tryable
		if (result.status() != TryExecutionResult.Status.SATISFIED) {
			// This is necessary since guided generation is not stopped automatically by falsified samples
			this.stopped = true;
		}
	}

	@Override
	public PropertyRunResult overridePropertyResult(PropertyRunResult originalResult) {
		if (originalResult.isAborted() || originalResult.isFailed()) {
			return originalResult;
		}
		var rejections = classifier.rejections();
		if (!rejections.isEmpty()) {
			String rejectionDetail = rejections.iterator().next();
			var failureReason = new AssertionFailedError(rejectionDetail);
			return originalResult.withStatus(PropertyRunResult.Status.FAILED)
								 .withFailureReason(failureReason);
		}
		return originalResult;
	}

	@Override
	public void stop() {
		this.stopped = true;
	}
}
