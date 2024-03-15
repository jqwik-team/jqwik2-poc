package jqwik2.api;

import java.nio.file.*;
import java.time.*;

import jqwik2.api.database.*;
import jqwik2.api.validation.*;

public class JqwikDefaults {

	private static FailureDatabase defaultFailureDatabase;

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

	public static Publisher defaultPublisher() {
		return new Publisher() {
			@Override
			public void report(String text) {
				System.out.print(text);
			}

			@Override
			public void reportLine(String text) {
				System.out.println(text);
			}

			@Override
			public boolean supportsAnsiCodes() {
				return true;
			}
		};
	}

	public static boolean publishOnlyFailedResults() {
		return false;
	}
}
