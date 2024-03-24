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
	public static TestDriver forProperty(PropertyDescription property) {
		return new TestDriver(property);
	}

	private final PropertyDescription property;
	private final List<String> tddSteps = new ArrayList<>();

	public TestDriver(PropertyDescription property) {
		this.property = property;
	}

	public void drive() {
		List<Generator<?>> generators = generators();
		Tryable tryable = safeTryable(property.condition());
		PropertyRunner runner = new PropertyRunner(generators, tryable);

		PropertyRunConfiguration statisticalRunConfiguration = buildRunConfiguration();
		var result = runner.run(statisticalRunConfiguration);

		publishResult(result);
		// updateTddDatabase(result);

	}

	private void publishResult(PropertyRunResult result) {
		PlatformPublisher publisher = PlatformPublisher.STDOUT;
		StringBuilder report = new StringBuilder();

		if (!tddSteps.isEmpty()) {
			report.append("%n  TDD Steps:%n");
			tddSteps.forEach(step -> report.append("  %s%n".formatted(step)));
		}


		report.append("%n  Status: %s%n".formatted(result.status()));
		if (result.status() == PropertyValidationStatus.FAILED && !result.falsifiedSamples().isEmpty()) {
			report.append(
				"  Sample <%s> failed:%n    %s%n".formatted(
					result.falsifiedSamples().first().values(),
					result.falsifiedSamples().first().throwable()
				)
			);
		}
		"Falsified: %s%n".formatted(result.status(), result.falsifiedSamples());
		publisher.publish(property.id(), report.toString());
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
				var tdd = TDD.start(property);
				try {
					var check = condition.check(args);
					collectTddStep(args, tdd, check);
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

	private void collectTddStep(List<Object> args, TDD tdd, boolean check) {
		Optional<String> label = tdd.label();
		String step = label
						  .map(l -> "[%s] %s: ".formatted(l, args))
						  .orElse("%s: ".formatted(args));
		if (check) {
			tddSteps.add(step + "Success");
		} else {
			tddSteps.add(step + "Failed");
		}
	}

}
