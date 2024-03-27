package jqwik2.tdd;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.support.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;

public class TestDriver {
	private static final String DEFAULT_CASE = "_";
	private TddTry lastTDD = null;

	public static TestDriver forProperty(PropertyDescription property) {
		return new TestDriver(property);
	}

	private final PropertyDescription property;
	private final Map<String, List<Pair<TryExecutionResult, Sample>>> tries = new HashMap<>();

	public TestDriver(PropertyDescription property) {
		this.property = property;
	}

	public void drive() {
		List<Generator<?>> generators = generators();
		Tryable tryable = safeTryable(property.condition());
		PropertyRunner runner = new PropertyRunner(generators, tryable);
		runner.onTryExecution((r, s) -> {
			if (lastTDD != null) {
				collectTddStep(lastTDD, s, r);
			}
		});

		PropertyRunConfiguration statisticalRunConfiguration = buildRunConfiguration();
		var result = runner.run(statisticalRunConfiguration);

		publishResult(result);
		// updateTddDatabase(result);

	}

	private void publishResult(PropertyRunResult result) {
		PlatformPublisher publisher = PlatformPublisher.STDOUT;
		StringBuilder report = new StringBuilder();

		appendTriesReport(report);
		appendStatus(result, report);
		appendAbortionReason(result, report);
		appendFailureReason(result, report);

		publisher.publish(property.id(), report.toString());
	}

	private static void appendStatus(PropertyRunResult result, StringBuilder report) {
		report.append("%n%nStatus: %s%n".formatted(result.status()));
	}

	private static void appendFailureReason(PropertyRunResult result, StringBuilder report) {
		if (result.status() == PropertyValidationStatus.FAILED && !result.falsifiedSamples().isEmpty()) {
			report.append(
				"  Sample <%s> failed:%n    %s%n".formatted(
					result.falsifiedSamples().first().values(),
					result.falsifiedSamples().first().throwable()
				)
			);
		}
	}

	private static void appendAbortionReason(PropertyRunResult result, StringBuilder report) {
		if (result.status() == PropertyValidationStatus.ABORTED) {
			String reason = result.abortionReason().map(Throwable::getMessage).orElseGet(() -> "Unknown abort reason");
			report.append("  Aborted: %s%n".formatted(reason));
		}
	}

	private void appendTriesReport(StringBuilder report) {
		for (var labelListOfPairs : tries.entrySet()) {
			var label = labelListOfPairs.getKey();
			var samples = labelListOfPairs.getValue();
			report.append("%n%s:".formatted(label));

			for (int i = 0; i < samples.size(); i++) {
				var pair = samples.get(i);
				var status = pair.first().status();
				var sample = pair.second();
				if (status == TryExecutionResult.Status.INVALID) {
					continue;
				}
				if (i > 0 && i < samples.size() - 1 && status == TryExecutionResult.Status.SATISFIED) {
					continue;
				}
				report.append("%n  %s: %s".formatted(sample.values(), status));
			}
		}
	}

	private PropertyRunConfiguration buildRunConfiguration() {
		return PropertyRunConfiguration.growing(1000, false, Duration.ofSeconds(10));
	}

	private List<Generator<?>> generators() {
		return property.arbitraries().stream().map(Arbitrary::generator).collect(Collectors.toList());
	}

	private Tryable safeTryable(Condition condition) {
		return Tryable.from(args -> {
			try {
				var tdd = TddTry.start(property);
				lastTDD = tdd;
				try {
					var check = condition.check(args);
					tdd.checkCovered();
					return check;
				} finally {
					tdd.finish();
				}
			} catch (Throwable t) {
				ExceptionSupport.rethrowIfBlacklisted(t);
				return ExceptionSupport.throwAsUnchecked(t);
			}
		});
	}

	private void collectTddStep(TddTry tdd, Sample sample, TryExecutionResult check) {
		Set<String> labels = tdd.labels();
		if (labels.isEmpty()) {
			var list = tries.computeIfAbsent(DEFAULT_CASE, k -> new ArrayList<>());
			list.add(Pair.of(check, sample));
			return;
		}
		for (String label : labels) {
			var list = tries.computeIfAbsent(label, k -> new ArrayList<>());
			list.add(Pair.of(check, sample));
			return;
		}
	}

}
