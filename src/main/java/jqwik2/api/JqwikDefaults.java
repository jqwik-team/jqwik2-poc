package jqwik2.api;

import java.time.*;

public class JqwikDefaults {

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
}
