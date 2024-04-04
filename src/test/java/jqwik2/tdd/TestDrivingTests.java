package jqwik2.tdd;

import java.time.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.validation.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class TestDrivingTests {

	@Example
	void fizzBuzz2() {

		var tddProperty =
			TDD.id("myId")
			   .forAll(Numbers.integers().between(1, 1_000_000))
			   .publisher(PlatformPublisher.STDOUT)
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

		TddDrivingResult result = tddProperty.drive();

		assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);
		assertThat(result.everythingCovered()).isTrue();
		assertThat(result.caseResults()).hasSize(3);
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

	@Example
	void stringContains() {
		var longStrings = Strings.strings().alpha().ofLength(10);
		var shortStrings = Strings.strings().alpha().ofLength(2);

		var tddProperty =
			TDD.forAll(longStrings, shortStrings)
			   .publisher(PlatformPublisher.STDOUT)
			   .verifyCase(
				   "contained",
				   (longString, shortString) -> longString.indexOf(shortString) >= 0,
				   (longString, shortString) -> assertThat(longString).contains(shortString)
			   )
			   .verifyCase(
				   "not contained",
				   (longString, shortString) -> longString.indexOf(shortString) < 0,
				   (longString, shortString) -> assertThat(longString).doesNotContain(shortString)
			   );

		TddDrivingStrategy strategy = TddDrivingStrategy.builder()
														.withMaxTries(0)
														.withMaxRuntime(Duration.ofSeconds(1))
														.build();
		TddDrivingResult result = tddProperty.drive(strategy);
	}
}