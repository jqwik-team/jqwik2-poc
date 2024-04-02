package jqwik2.tdd;

import jqwik2.api.arbitraries.*;
import jqwik2.api.description.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class TestDrivingTests {

	@Example
		// @Disabled("Not yet implemented")
	void fizzBuzz2() {

		TddProperty.P1<Integer> tddProperty =
			TDD.id("myId")
			   .forAll(Numbers.integers().between(1, 1_000_000))
			   .verifyCase(
				   "normal number", i -> i % 3 != 0 && i % 5 != 0,
				   i -> {
					   var s = fizzBuzz(i);
					   assertThat(s).isEqualTo(Integer.toString(i));
				   }
			   ).verifyCase(
				   "divisible by 3", i -> i % 3 == 0,
				   i -> {
					   var s = fizzBuzz(i);
					   assertThat(s).startsWith("Fizz");
				   }
			   ).verifyCase(
				   "divisible by 5", i -> i % 5 == 0,
				   i -> {
					   var s = fizzBuzz(i);
					   assertThat(s).endsWith("Buzz");
				   }
			   );

		TddResult result = tddProperty.drive();

		// assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);
		// assertThat(result.everythingCovered()).isFalse();
		// assertThat(result.caseResults()).hasSize(1);
	}

	private String fizzBuzz(int i) {
		String result = "";
		// if (i == 3) {
		if (i % 3 == 0) {
			result += "Fizz";
		}
		if (i % 5 == 0) {
			result += "Buzz";
		}
		if (result.isEmpty()) {
			result = Integer.toString(i);
		}
		return result;
	}
}
