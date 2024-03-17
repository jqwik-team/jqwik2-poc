package jqwik2.internal.validation;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.description.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;
import jqwik2.api.validation.*;
import jqwik2.internal.*;
import jqwik2.internal.reporting.*;
import jqwik2.internal.statistics.*;

import static jqwik2.internal.PropertyRunConfiguration.*;

public class PropertyValidatorImpl implements PropertyValidator {

	private final PropertyDescription property;
	private final ReportSection parametersReport = new ReportSection("parameters");
	private final ReportSection resultReport = new ReportSection("result");

	private FailureDatabase database;
	private PlatformPublisher platformPublisher;
	private boolean publishSuccessfulResults;

	public PropertyValidatorImpl(PropertyDescription property) {
		this.property = property;
		this.database = JqwikDefaults.defaultFailureDatabase();
		this.platformPublisher = JqwikDefaults.defaultPlatformPublisher();
		this.publishSuccessfulResults = JqwikDefaults.defaultPublishSuccessfulResults();
	}

	@Override
	public PropertyValidationResult validate(PropertyValidationStrategy strategy) {
		Set<ClassifyingCollector<List<Object>>> collectors = asClassifyingCollectors(property.classifiers());

		PropertyRunResult result = run(strategy, collectors);
		executeResultCallbacks(result);

		PropertyValidationResult validationResult = new PropertyValidationResultFacade(result);

		publishClassifyingReports(collectors);

		if (shouldPublishResult(validationResult.status())) {
			publishRunReport(validationResult);
		}

		// TODO: Report falsified samples

		return validationResult;
	}

	private PropertyRunResult run(PropertyValidationStrategy strategy, Set<ClassifyingCollector<List<Object>>> collectors) {
		List<Generator<?>> generators = generators(strategy);
		Tryable tryable = safeTryable(property.condition(), collectors);
		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunConfiguration runConfiguration = buildRunConfiguration(generators, collectors, strategy);
		return propertyRun.run(runConfiguration);
	}

	private void publishRunReport(PropertyValidationResult result) {

		fillInResultReport(result);

		StringBuilder report = new StringBuilder();

		result.failure().ifPresent(failure -> publishFailure(failure, report));
		resultReport.publish(report);
		parametersReport.publish(report);

		String reportKey = "%s (%s)".formatted(property.id(), result.status().name());
		platformPublisher.publish(reportKey, report.toString());
	}

	private void fillInResultReport(PropertyValidationResult result) {
		resultReport.append("status", result.status().name());
		result.failure()
			  .ifPresent(failure -> resultReport.append("failure", failure.getClass().getName()));
		resultReport.append("# tries", result.countTries());
		resultReport.append("# checks", result.countChecks());
	}

	private void publishFailure(Throwable throwable, StringBuilder report) {
		String assertionClass = throwable.getClass().getName();
		report.append("%n  %s".formatted(assertionClass));
		List<String> assertionMessageLines = throwable.getMessage().lines().toList();
		if (assertionMessageLines.isEmpty()) {
			return;
		}
		report.append(":%n".formatted());
		for (String line : assertionMessageLines) {
			if (line.isBlank()) continue;
			report.append("    %s%n".formatted(line));
		}
		report.append("%n".formatted());
	}

	private boolean shouldPublishResult(PropertyValidationStatus status) {
		return status != PropertyValidationStatus.SUCCESSFUL || publishSuccessfulResults;
	}

	private List<Generator<?>> generators(PropertyValidationStrategy strategy) {
		List<Generator<?>> generators = new ArrayList<>();
		var edgeCasesMode = strategy.edgeCases();
		Optional<Generator.DecoratorFunction> edgeCasesDecorator = edgeCasesDecorator(edgeCasesMode);
		for (Arbitrary<?> a : property.arbitraries()) {
			Generator<?> toDecorate = a.generator();
			if (edgeCasesDecorator.isPresent()) {
				toDecorate = edgeCasesDecorator.get().apply(toDecorate);
			}
			generators.add(toDecorate);
		}
		return generators;
	}

	private Tryable safeTryable(Condition condition, Set<ClassifyingCollector<List<Object>>> collectors) {
		return Tryable.from(args -> {
			try {
				collectors.forEach(c -> c.classify(args));
				return condition.check(args);
			} catch (Throwable t) {
				ExceptionSupport.rethrowIfBlacklisted(t);
				return ExceptionSupport.throwAsUnchecked(t);
			}
		});
	}

	private static Optional<Generator.DecoratorFunction> edgeCasesDecorator(PropertyValidationStrategy.EdgeCasesMode edgeCasesMode) {
		return switch (edgeCasesMode) {
			// TODO: Calculate edge cases probability and maxEdgeCases from maxTries
			case MIXIN -> Optional.of(WithEdgeCasesDecorator.function(0.05, 100));
			case OFF -> Optional.empty();
			case null -> throw new IllegalArgumentException("Edge cases mode must not be null");
		};
	}

	private PropertyRunConfiguration buildRunConfiguration(
		List<Generator<?>> generators,
		Set<ClassifyingCollector<List<Object>>> collectors,
		PropertyValidationStrategy strategy
	) {
		var plainRunConfiguration = new RunConfigurationBuilder(property.id(), generators, strategy, database).build(parametersReport);

		plainRunConfiguration.effectiveSeed()
							 .ifPresent(seed -> parametersReport.append("seed", seed)
							 );
		parametersReport.append("max tries", strategy.maxTries());
		parametersReport.append("max runtime", strategy.maxRuntime());
		parametersReport.append("shrinking", strategy.shrinking());
		parametersReport.append("edge cases", strategy.edgeCases());
		parametersReport.append("concurrency", strategy.concurrency());

		if (!isCoverageCheckPresent(collectors)) {
			return plainRunConfiguration;
		}
		return wrapWithClassifierGuidance(plainRunConfiguration, collectors);
	}

	private static boolean isCoverageCheckPresent(Set<ClassifyingCollector<List<Object>>> collectors) {
		return collectors.stream().anyMatch(c -> c.hasCoverageCheck());
	}

	private PropertyRunConfiguration wrapWithClassifierGuidance(
		PropertyRunConfiguration plainRunConfiguration,
		Set<ClassifyingCollector<List<Object>>> collectors
	) {
		return wrapSource(
			plainRunConfiguration,
			source -> new StatisticallyGuidedGenerationSource(source, collectors, JqwikDefaults.defaultStandardDeviationThreshold())
		);
	}

	private void publishClassifyingReports(Set<ClassifyingCollector<List<Object>>> collectors) {
		for (var classifier : collectors) {
			var header = "classifier (%d)".formatted(classifier.total());
			var classifierSection = new ReportSection(header);
			for (String label : classifier.labels()) {
				var minPercentage = classifier.minPercentage(label);
				var minPercentagePart = minPercentage > 0.0 ? " (>= %.2f%%)".formatted(minPercentage) : "";
				String key = "%s (%d)".formatted(label, classifier.count(label));
				String value = "%.2f%%%s".formatted(classifier.percentage(label), minPercentagePart);
				classifierSection.append(key, value);
			}
			String key = "%s: %s".formatted(property.id(), header);
			classifierSection.publish(key, platformPublisher);
		}
	}

	private Set<ClassifyingCollector<List<Object>>> asClassifyingCollectors(List<Classifier> classifiers) {
		return classifiers.stream()
						  .map(this::asClassifyingCollector)
						  .collect(Collectors.toSet());
	}

	private ClassifyingCollector<List<Object>> asClassifyingCollector(Classifier classifier) {
		var collector = new ClassifyingCollector<List<Object>>();
		for (Classifier.Case<?> aCase : classifier.cases()) {
			collector.addCase(aCase.label(), aCase.minPercentage(), asPredicate(aCase));
		}
		return collector;
	}

	private static Predicate<List<Object>> asPredicate(Classifier.Case<?> aCase) {
		return args -> {
			try {
				return aCase.condition().check(args);
			} catch (Throwable e) {
				return false;
			}
		};
	}

	private void executeResultCallbacks(PropertyRunResult result) {
		switch (result.status()) {
			case SUCCESSFUL:
				onSuccessful();
				break;
			case FAILED:
				onFailed(result);
				break;
			case ABORTED:
				break;
		}
	}

	private void onSuccessful() {
		database.deleteProperty(property.id());
	}

	private void onFailed(PropertyRunResult result) {
		saveFailureToDatabase(result);
	}

	private void saveFailureToDatabase(PropertyRunResult result) {
		Set<SampleRecording> sampleRecordings = result.falsifiedSamples().stream()
													  .map(s -> s.sample().recording())
													  .collect(Collectors.toSet());
		database.saveFailure(property.id(), result.effectiveSeed().orElse(null), sampleRecordings);
	}

	@Override
	public PropertyValidator failureDatabase(FailureDatabase database) {
		this.database = database;
		return this;
	}

	@Override
	public PropertyValidator publisher(PlatformPublisher publisher) {
		this.platformPublisher = publisher;
		return this;
	}

	@Override
	public PropertyValidator publishSuccessfulResults(boolean publishSuccessfulResults) {
		this.publishSuccessfulResults = publishSuccessfulResults;
		return this;
	}
}
