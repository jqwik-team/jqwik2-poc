package jqwik2.api;

import java.nio.file.*;
import java.time.*;

import jqwik2.api.database.*;
import jqwik2.api.validation.*;
import jqwik2.tdd.*;

public class JqwikDefaults {

	private static FailureDatabase defaultFailureDatabase;

	private static TddDatabase tddDatabase;

	public static int defaultMaxTries() {
		return 100;
	}

	public static Duration defaultMaxDuration() {
		return Duration.ofMinutes(10);
	}

	public static boolean defaultFilterOutDuplicateSamples() {
		return false;
	}

	public static PropertyValidationStrategy.ShrinkingMode defaultShrinkingMode() {
		return PropertyValidationStrategy.ShrinkingMode.FULL;
	}

	public static PropertyValidationStrategy.GenerationMode defaultGenerationMode() {
		return PropertyValidationStrategy.GenerationMode.SMART;
	}

	public static PropertyValidationStrategy.EdgeCasesMode defaultEdgeCasesMode() {
		return PropertyValidationStrategy.EdgeCasesMode.MIXIN;
	}

	public static FailureDatabase defaultFailureDatabase() {
		if (defaultFailureDatabase == null) {
			try {
				defaultFailureDatabase = new DirectoryBasedFailureDatabase(Path.of(".jqwik", "failures"));
			} catch (Exception e) {
				System.err.printf("Could not create default failure database: %s%n", e.getMessage());
				defaultFailureDatabase = FailureDatabase.NULL;
			}
		}
		return defaultFailureDatabase;
	}

	public static PropertyValidationStrategy.AfterFailureMode defaultAfterFailureMode() {
		return PropertyValidationStrategy.AfterFailureMode.SAMPLES_ONLY;
	}

	public static PropertyValidationStrategy.ConcurrencyMode defaultConcurrencyMode() {
		return PropertyValidationStrategy.ConcurrencyMode.SINGLE_THREAD;
	}

	public static double defaultStandardDeviationThreshold() {
		return 3.0;
	}

	public static PlatformPublisher defaultPlatformPublisher() {
		return PlatformPublisher.NULL;
	}

	public static boolean defaultPublishSuccessfulResults() {
		return true;
	}

	public static TddDatabase defaultTddDatabase() {
		if (tddDatabase == null) {
			try {
				tddDatabase = new DirectoryBasedTddDatabase(Path.of(".jqwik", "tdd"));
			} catch (Exception e) {
				System.err.printf("Could not create tdd database: %s%n", e.getMessage());
				tddDatabase = TddDatabase.NULL;
			}
		}
		return tddDatabase;
	}

}
