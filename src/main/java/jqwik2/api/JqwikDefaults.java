package jqwik2.api;

import java.nio.file.*;
import java.time.*;

import jqwik2.api.database.*;

public class JqwikDefaults {

	private static FailureDatabase defaultFailureDatabase;

	public static int defaultMaxTries() {
		return 100;
	}

	public static Duration defaultMaxDuration() {
		return Duration.ofMinutes(10);
	}

	public static PropertyRunStrategy.ShrinkingMode defaultShrinkingMode() {
		return PropertyRunStrategy.ShrinkingMode.FULL;
	}

	public static PropertyRunStrategy.GenerationMode defaultGenerationMode() {
		return PropertyRunStrategy.GenerationMode.SMART;
	}

	public static PropertyRunStrategy.EdgeCasesMode defaultEdgeCasesMode() {
		return PropertyRunStrategy.EdgeCasesMode.MIXIN;
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

	static PropertyRunStrategy.AfterFailureMode defaultAfterFailureMode() {
		return PropertyRunStrategy.AfterFailureMode.SAMPLES_ONLY;
	}

	static boolean defaultFilterOutDuplicateSamples() {
		return true;
	}
}
