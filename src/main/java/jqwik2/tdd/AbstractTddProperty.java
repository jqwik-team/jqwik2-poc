package jqwik2.tdd;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;
import jqwik2.api.validation.*;
import jqwik2.internal.reporting.*;

abstract class AbstractTddProperty<P extends TddProperty<P>> implements TddProperty<P> {
	protected final String id;
	private final List<TddCase> cases = new ArrayList<>();
	private final Map<TddCase, TddRecord> records = new LinkedHashMap<>();
	private PlatformPublisher publisher = JqwikDefaults.defaultPlatformPublisher();

	AbstractTddProperty(String id) {
		this.id = id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public P publisher(PlatformPublisher publisher) {
			this.publisher = publisher;
			return (P) this;
	}

	@Override
	public TddDrivingResult drive(TddDrivingStrategy strategy) {
		List<PropertyValidationResult> caseResults = runAllTddCases(strategy);

		PropertyValidationStatus status = PropertyValidationStatus.SUCCESSFUL;
		if (caseResults.stream().anyMatch(r -> !r.isSuccessful())) {
			status = PropertyValidationStatus.FAILED;
		}

		PropertyValidationResult everythingCoveredResult = validateEverythingCovered(strategy);
		boolean isEverythingCovered = everythingCoveredResult.isSuccessful();

		if (!isEverythingCovered) {
			publishNotEverythingCoveredReport(everythingCoveredResult);
		}

		var drivingResult = new TddDrivingResult(status, caseResults, isEverythingCovered);
		if (JqwikDefaults.defaultPublishSuccessfulResults() || status != PropertyValidationStatus.SUCCESSFUL) {
			publishDrivingResult(drivingResult);
		}
		return drivingResult;
	}

	private void publishNotEverythingCoveredReport(PropertyValidationResult everythingCoveredResult) {
		StringBuilder report = new StringBuilder();
		report.append("%nNOT COVERED:%n".formatted());
		everythingCoveredResult.falsifiedSamples().forEach(
			s -> report.append("  %s%n".formatted(s.sample().regenerateValues()))
		);
		publisher.publish(id, report.toString());
	}

	private List<PropertyValidationResult> runAllTddCases(TddDrivingStrategy strategy) {
		List<PropertyValidationResult> caseResults = new ArrayList<>();
		for (var tddCase : cases) {
			var validator =
				PropertyValidator.forProperty(tddCase.property())
								 .failureDatabase(FailureDatabase.NULL)
								 .publisher(PlatformPublisher.NULL)
								 // .publisher(PlatformPublisher.STDOUT)
								 .registerTryExecutionListener((r, s) -> collectTddStep(tddCase, r, s));
			var result = validator.validate(buildRunConfiguration(strategy));
			caseResults.add(result);
			publishRecord(tddCase);
		}
		return caseResults;
	}

	protected abstract PropertyDescription everythingCoveredProperty(
		String everythingCoveredPropertyId,
		List<Condition> allCaseConditions
	);

	protected String testCaseId(String label) {
		return "%s:%s".formatted(id, label.trim());
	}

	protected void addTestCase(String label, PropertyDescription property) {
		cases.add(new TddCase(label, property));
	}

	private void publishDrivingResult(TddDrivingResult drivingResult) {
		var resultReport = new ReportSection("result");
		resultReport.append("status", drivingResult.status());
		resultReport.append("cases", drivingResult.caseResults().size());
		// drivingResult.caseResults().forEach(caseResult -> resultReport.append(caseResult., caseResult.status()

		resultReport.append("all values covered", drivingResult.everythingCovered());
		resultReport.publish(id, publisher);
	}

	private void publishRecord(TddCase tddCase) {
		var record = records.get(tddCase);
		var report = new StringBuilder();
		if (record != null) {
			record.publish(report);
		}
		publisher.publish(tddCase.property().id(), report.toString());
	}

	private void collectTddStep(TddCase tddCase, TryExecutionResult r, Sample s) {
		if (r.status() == TryExecutionResult.Status.INVALID) {
			return;
		}
		updateRecord(tddCase, r, s);
	}

	private void updateRecord(TddCase tddCase, TryExecutionResult result, Sample sample) {
		var record = records.computeIfAbsent(tddCase, tddC -> new TddRecord(tddC.label()));
		record.update(result, sample);
	}

	private PropertyValidationResult validateEverythingCovered(TddDrivingStrategy strategy) {
		String everythingCoveredId = id + ":everythingCoveredResult";
		var allCaseConditions = cases.stream().map(c -> c.property().condition()).toList();
		var property = everythingCoveredProperty(everythingCoveredId, allCaseConditions);
		var validator = PropertyValidator.forProperty(property)
										 .failureDatabase(FailureDatabase.NULL)
										 .publisher(PlatformPublisher.NULL);
		return validator.validate(buildRunConfiguration(strategy));
	}

	private PropertyValidationStrategy buildRunConfiguration(TddDrivingStrategy strategy) {
		return PropertyValidationStrategy.builder()
										 .withGeneration(PropertyValidationStrategy.GenerationMode.GROWING)
										 .withMaxTries(strategy.maxTries())
										 .withMaxRuntime(strategy.maxRuntime())
										 .build();
	}

}
