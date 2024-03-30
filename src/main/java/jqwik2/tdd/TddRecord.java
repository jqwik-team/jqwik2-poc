package jqwik2.tdd;

import jqwik2.api.*;

class TddRecord {
	private final String label;

	private Sample firstSatisfiedSample;
	private Sample lastSatisfiedSample;
	private int countSatisfied = 0;
	private Sample falsifiedSample;
	private Throwable falsificationCause;

	TddRecord(String label) {
		this.label = label;
	}

	void update(TryExecutionResult result, Sample sample) {
		if (result.status() == TryExecutionResult.Status.SATISFIED) {
			countSatisfied++;
			if (countSatisfied == 1) {
				firstSatisfiedSample = sample;
			}
			lastSatisfiedSample = sample;
		} else if (result.status() == TryExecutionResult.Status.FALSIFIED) {
			falsifiedSample = sample;
			falsificationCause = result.throwable();
		}
	}

	void publish(StringBuilder report) {
		report.append("%n%s:".formatted(label));
		if (countSatisfied > 0) {
			report.append("%n  Satisfied: %d times".formatted(countSatisfied));
			if (countSatisfied == 1) {
				report.append("%n    %s".formatted(firstSatisfiedSample.values()));
			} else {
				report.append("%n    First: %s".formatted(firstSatisfiedSample.values()));
				report.append("%n    Last:  %s".formatted(lastSatisfiedSample.values()));
			}
		}
		if (falsifiedSample != null) {
			report.append("%n  Falsified:".formatted());
			report.append("%n    %s".formatted(falsifiedSample.values()));
			report.append("%n    %s".formatted(falsificationCause));
		}
		report.append("%n".formatted());
	}
}
