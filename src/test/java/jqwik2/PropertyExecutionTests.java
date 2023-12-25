package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.Assume;
import jqwik2.api.PropertyExecutionResult.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class PropertyExecutionTests {

	@Example
	void runSuccessfulProperty() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> true);

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			10,
			0.0
		);

		PropertyExecutionResult result = propertyCase.execute();
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		assertThat(result.countChecks()).isEqualTo(10);
	}

	@Example
	void runSuccessfulWithInvalids() {
		List<Generator<?>> generators = List.of(
			new IntegerGenerator(0, 100)
		);
		Tryable tryable = Tryable.from(args -> {
			int first = (int) args.get(0);
			Assume.that(first % 2 == 0);
			return true;
		});

		PropertyCase propertyCase = new PropertyCase(
			generators,
			tryable,
			"42",
			10,
			0.0
		);

		PropertyExecutionResult result = propertyCase.execute();
		assertThat(result.status()).isEqualTo(Status.SUCCESSFUL);
		assertThat(result.countTries()).isEqualTo(10);
		// 2 invalids - depends on random seed
		assertThat(result.countChecks()).isEqualTo(8);
	}
}
