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

	private FailureDatabase database;
	private StatisticallyGuidedGenerationSource classifyingSource = null;

	public PropertyValidatorImpl(PropertyDescription property) {
		this.property = property;
		this.database = JqwikDefaults.defaultFailureDatabase();
		this.reporter = new DefaultReporter(JqwikDefaults.defaultPublisher());
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

		// TODO: Report falsified samples

		reportClassifierResults(collectors);

		publishFinalReport();

		return propertyRunResult;
	}

	private void publishFinalReport() {
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

	private void reportClassifierResults(Set<ClassifyingCollector<List<Object>>> collectors) {
		if (collectors.isEmpty()) {
			return;
		}
		for (var classifier : collectors) {
			// TODO: Hand in report with labels, counts, percentages and minPercentages
			reporter.appendToReport(Reporter.CATEGORY_CLASSIFIER, Integer.toString(classifier.total()), classifier.percentages());
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
