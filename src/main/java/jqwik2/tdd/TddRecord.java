package jqwik2.tdd;

import java.util.*;

import jqwik2.api.*;

class TddRecord {
	private final String label;

	private List<Sample> satisfiedSamples = new ArrayList<>();
	private Sample falsifiedSample;
	private Throwable falsificationCause;

	TddRecord(String label) {
		this.label = label;
	}

	void update(TryExecutionResult result, Sample sample) {
		if (result.status() == TryExecutionResult.Status.SATISFIED) {
			satisfiedSamples.add(sample);
		} else if (result.status() == TryExecutionResult.Status.FALSIFIED) {
			falsifiedSample = sample;
			falsificationCause = result.throwable();
		}
	}

	void publish(StringBuilder report) {
		report.append("%n%s:".formatted(label));
		for (int i = 0; i < satisfiedSamples.size(); i++) {
			Sample sample = satisfiedSamples.get(i);
			if (shouldBeReported(sample, i)) {
				report.append("%n  %s: SATISFIED".formatted(sample.values()));
			}
		}
		if (falsifiedSample != null) {
			report.append("%n  %s: FALSIFIED".formatted(falsifiedSample.values()));
			report.append("%n    %s".formatted(falsificationCause));
		}
		report.append("%n".formatted());
	}

	private boolean shouldBeReported(Sample sample, int index) {
		return index == 0 || index == satisfiedSamples.size() - 1;
	}
}
