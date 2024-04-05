package jqwik2.internal.validation;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;
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
	private final List<BiConsumer<TryExecutionResult, Sample>> tryExecutionListeners = new ArrayList<>();

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

		PropertyRunResult result = runStrictValidation(strategy, collectors);

		PropertyValidationResult validationResult = new PropertyValidationResultFacade(result);

		publishClassifyingReports(collectors);

		if (shouldPublishResult(validationResult.status())) {
			publishRunReport(validationResult, true);
		}

		return validationResult;
	}

	@Override
	public PropertyValidationResult validateStatistically(
		double minPercentage,
		double maxStandardDeviationFactor,
		PropertyValidationStrategy strategy
	) {
		if (!property.classifiers().isEmpty()) {
			throw new IllegalStateException("Statistical validation cannot be combined with classifiers");
		}

		PropertyRunResult result = runStatisticalValidation(minPercentage, maxStandardDeviationFactor, strategy);
		PropertyValidationResult validationResult = new PropertyValidationResultFacade(result, true);

		if (shouldPublishResult(validationResult.status())) {
			publishRunReport(validationResult, false);
		}

		return validationResult;
	}

	private void updateFailureDatabaseForStatisticalValidation(PropertyRunResult result, Optional<String> effectiveSeed) {
		if (result.status() == PropertyValidationStatus.SUCCESSFUL) {
			database.deleteProperty(property.id());
		} else if (result.status() == PropertyValidationStatus.FAILED) {
			database.saveFailure(property.id(), effectiveSeed.orElse(null), Set.of());
		}
	}

	private PropertyRunResult runStatisticalValidation(
		double minPercentage,
		double maxStandardDeviationFactor,
		PropertyValidationStrategy strategy
	) {
		List<Generator<?>> generators = generators(strategy.edgeCases());
		Tryable tryable = safeTryable(property.invariant(), Set.of());
		PropertyRunner runner = createRunner(generators, tryable);

		PropertyRunConfiguration statisticalRunConfiguration = buildStatisticalRunConfiguration(minPercentage, maxStandardDeviationFactor, strategy, generators);
		var result = runner.run(statisticalRunConfiguration);
		updateFailureDatabaseForStatisticalValidation(result, statisticalRunConfiguration.effectiveSeed());
		return result;
	}

	private PropertyRunConfiguration buildStatisticalRunConfiguration(
		double minPercentage,
		double maxStandardDeviationFactor,
		PropertyValidationStrategy strategy,
		List<Generator<?>> generators
	) {
		String validationLabel = "STATISTICAL(%s, %s)".formatted(minPercentage, maxStandardDeviationFactor);
		parametersReport.append("validation", validationLabel);

		PropertyRunConfiguration runConfiguration = new RunConfigurationBuilder(property.id(), generators, strategy, database)
														.forStatisticalCheck()
														.build(parametersReport);

		return wrapWithStatisticalCheck(runConfiguration, minPercentage, maxStandardDeviationFactor);
	}

	private PropertyRunConfiguration wrapWithStatisticalCheck(
		PropertyRunConfiguration plainConfiguration,
		double minPercentage,
		double maxStandardDeviationFactor
	) {
		return wrapSource(
			plainConfiguration,
			source -> new StatisticalPropertySource(source, minPercentage, maxStandardDeviationFactor)
		);
	}

	private void publishFalsifiedSamples(SortedSet<FalsifiedSample> falsifiedSamples, StringBuilder report) {
		if (falsifiedSamples.isEmpty()) {
			return;
		}
		report.append("%n".formatted());
		report.append("%n".formatted());

		publishOriginalSample(falsifiedSamples, report);
		if (falsifiedSamples.size() == 1) {
			return;
		}
		report.append("%n".formatted());
		publishSmallestSample(falsifiedSamples, report);
	}

	private void publishSmallestSample(SortedSet<FalsifiedSample> falsifiedSamples, StringBuilder report) {
		FalsifiedSample smallestFalsifiedSample = falsifiedSamples.first();
		var label = "Smallest Falsified Sample (%d steps)".formatted(smallestFalsifiedSample.countShrinkingSteps());
		publishSample(label, smallestFalsifiedSample, report);
	}

	private void publishOriginalSample(SortedSet<FalsifiedSample> falsifiedSamples, StringBuilder report) {
		FalsifiedSample originalFalsifiedSample = falsifiedSamples.last();
		publishSample("Original Falsified Sample", originalFalsifiedSample, report);
	}

	private void publishSample(String label, FalsifiedSample sample, StringBuilder report) {
		report.append("%s%n".formatted(label));
		new LineReporter(report::append).appendUnderline(0, label.length());
		var argsReport = new LineReporter(report::append, 1);
		var values = sample.sample().regenerateValues();
		// TODO: Report differences between original and regenerated sample values
		for (int index = 0; index < values.size(); index++) {
			var arg = values.get(index);
			argsReport.appendLn(0, "arg-%d: %s".formatted(index, arg));
		}
	}

	private PropertyRunResult runStrictValidation(PropertyValidationStrategy strategy, Set<ClassifyingCollector<List<Object>>> collectors) {
		List<Generator<?>> generators = generators(strategy.edgeCases());
		Tryable tryable = safeTryable(property.invariant(), collectors);
		PropertyRunner runner = createRunner(generators, tryable);

		PropertyRunConfiguration runConfiguration = buildRunConfiguration(generators, collectors, strategy);
		var propertyRunResult = runner.run(runConfiguration);
		updateFailureDatabase(propertyRunResult, runConfiguration.effectiveSeed());

		return propertyRunResult;
	}

	private PropertyRunner createRunner(List<Generator<?>> generators, Tryable tryable) {
		PropertyRunner propertyRunner = new PropertyRunner(generators, tryable);
		tryExecutionListeners.forEach(propertyRunner::registerTryExecutionListener);
		return propertyRunner;
	}

	private void publishRunReport(PropertyValidationResult result, boolean publishFalsifiedSamples) {
		fillInResultReport(result);

		StringBuilder report = new StringBuilder();

		result.failure().ifPresent(failure -> publishFailure(failure, report));
		resultReport.publish(report);
		parametersReport.publish(report);

		if (publishFalsifiedSamples) {
			publishFalsifiedSamples(result.falsifiedSamples(), report);
		}

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
		var message = throwable.getMessage();
		List<String> assertionMessageLines = message == null ? List.of() : message.lines().toList();
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

	private List<Generator<?>> generators(PropertyValidationStrategy.EdgeCasesMode edgeCasesMode) {
		List<Generator<?>> generators = new ArrayList<>();
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
		String validationLabel = "STRICT";
		parametersReport.append("validation", validationLabel);

		var plainRunConfiguration = new RunConfigurationBuilder(property.id(), generators, strategy, database).build(parametersReport);

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

	private void updateFailureDatabase(PropertyRunResult result, Optional<String> effectiveSeed) {
		if (result.status() == PropertyValidationStatus.SUCCESSFUL) {
			database.deleteProperty(property.id());
		} else if (result.status() == PropertyValidationStatus.FAILED) {
			saveFailureToDatabase(result, effectiveSeed);
		}
	}

	private void saveFailureToDatabase(PropertyRunResult result, Optional<String> effectiveSeed) {
		Set<SampleRecording> sampleRecordings = result.falsifiedSamples().stream()
													  .map(s -> s.sample().recording())
													  .collect(Collectors.toSet());
		database.saveFailure(property.id(), effectiveSeed.orElse(null), sampleRecordings);
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

	@Override
	public PropertyValidator registerTryExecutionListener(BiConsumer<TryExecutionResult, Sample> tryExecutionListener) {
		tryExecutionListeners.add(tryExecutionListener);
		return this;
	}
}
