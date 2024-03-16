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
import jqwik2.internal.statistics.*;

import static jqwik2.internal.PropertyRunConfiguration.*;

public class PropertyValidatorImpl implements PropertyValidator {

	private final PropertyDescription property;
	private final DefaultReporter reporter;
	private final Publisher publisher;

	private FailureDatabase database;

	public PropertyValidatorImpl(PropertyDescription property) {
		this.property = property;
		this.database = JqwikDefaults.defaultFailureDatabase();
		this.publisher = JqwikDefaults.defaultPublisher();
		this.reporter = new DefaultReporter(publisher);
	}

	@Override
	public PropertyValidationResult validate(PropertyValidationStrategy strategy) {
		PropertyRunResult result = run(strategy);
		executeResultCallbacks(result);
		return new PropertyValidationResultFacade(result);
	}

	private PropertyRunResult run(PropertyValidationStrategy strategy) {
		List<Generator<?>> generators = generators(strategy);
		Set<ClassifyingCollector<List<Object>>> collectors = asClassifyingCollectors(property.classifiers());
		Tryable tryable = safeTryable(property.condition(), collectors);
		PropertyRun propertyRun = new PropertyRun(generators, tryable);

		PropertyRunConfiguration runConfiguration = buildRunConfiguration(generators, collectors, strategy);
		// TODO: Report configuration parameters (generation, maxTries, maxDuration, shrinking, edge cases, concurrency)

		// TODO: Let propertyRun report configuration parameter (seed)
		var propertyRunResult = propertyRun.run(runConfiguration);

		// TODO: Report result (status, tries, checks, time)

		publishRunReport();

		publishClassifyingReports(collectors);

		// TODO: Report falsified samples

		return propertyRunResult;
	}

	private void publishRunReport() {
		reporter.publishReport();
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

	private PropertyRunConfiguration buildRunConfiguration(List<Generator<?>> generators,
														   Set<ClassifyingCollector<List<Object>>> collectors,
														   PropertyValidationStrategy strategy) {
		var plainRunConfiguration = new RunConfigurationBuilder(property.id(), generators, strategy, database).build();
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
			publisher.reportLine("");
			publisher.reportLine("|--classifier (%d)--|".formatted(classifier.total()));
			for (String label : classifier.labels()) {
				var minPercentage = classifier.minPercentage(label);
				var minPercentagePart = minPercentage > 0.0 ? " (>= %.2f%%)".formatted(minPercentage) : "";
				publisher.reportLine("  %s (%d) | %.2f%%%s".formatted(
					label,
					classifier.count(label),
					classifier.percentage(label),
					minPercentagePart
				));
			}
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
	public void failureDatabase(FailureDatabase database) {
		this.database = database;
	}
}
