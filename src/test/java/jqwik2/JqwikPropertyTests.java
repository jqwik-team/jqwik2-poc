package jqwik2;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class JqwikPropertyTests {

	@Example
	void propertyWith1ParameterSucceeds() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(Numbers.integers()).check(i -> true);
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);

		result = property.forAll(Numbers.integers()).verify(i -> {
			// Thread.sleep(10);
			// System.out.println(i);
		});
		assertThat(result.isSuccessful()).isTrue();
		assertThat(result.countTries()).isEqualTo(100);
		assertThat(result.countChecks()).isEqualTo(100);
	}

	@Example
	void propertyWith1ParameterFails() {
		var property = new JqwikProperty();

		PropertyRunResult result = property.forAll(Numbers.integers()).check(i -> false);
		assertThat(result.isFailed()).isTrue();
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);

		result = property.forAll(Numbers.integers()).verify(i -> {
			// Thread.sleep(10);
			// System.out.println(i);
			throw new AssertionError("failed");
		});
		assertThat(result.isFailed()).isTrue();
		assertThat(result.countTries()).isEqualTo(1);
		assertThat(result.countChecks()).isEqualTo(1);
	}
}
