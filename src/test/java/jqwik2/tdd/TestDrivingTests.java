package jqwik2.tdd;

import java.nio.file.*;
import java.time.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.api.recording.*;
import jqwik2.api.validation.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class TestDrivingTests {

	@Example
	void tddDatabase() {
		Path localPath = Paths.get("src", "test", "java", "jqwik2", "tdd", ".tdd");
		var database = new DirectoryBasedTddDatabase(localPath);
		database.clear();

		var recording = new SampleRecording(Recording.choice(42));
		var recording2 = new SampleRecording(Recording.choice(41));
		database.saveSample("myId", "case1", recording);
		var samples = database.loadSamples("myId", "case1");
		assertThat(samples).hasSize(1);
		assertThat(samples).contains(recording);
		assertThat(database.isSamplePresent("myId", "case1", recording)).isTrue();
		assertThat(database.isSamplePresent("myId", "case1", recording2)).isFalse();
	}

	@Example
	void fizzBuzz() {

		var tddProperty =
			TDD.id("fizzBuzz")
			   .forAll(Numbers.integers().between(1, 1_000_000))
			   .publisher(PlatformPublisher.STDOUT)
			   .verifyCase(
				   "normal number", i -> i % 3 != 0 && i % 5 != 0,
				   i -> {
					   var s = fizzBuzzDone(i);
					   assertThat(s).isEqualTo(Integer.toString(i));
				   }
			   ).verifyCase(
				   "divisible by 3", i -> i % 3 == 0,
				   i -> {
					   var s = fizzBuzzDone(i);
					   assertThat(s).startsWith("Fizz");
				   }
			   ).verifyCase(
				   "divisible by 5", i -> i % 5 == 0,
				   i -> {
					   var s = fizzBuzzDone(i);
					   assertThat(s).endsWith("Buzz");
				   }
			   );

		TddDrivingResult result = tddProperty.drive();

		assertThat(result.status()).isEqualTo(PropertyValidationStatus.SUCCESSFUL);
		assertThat(result.everythingCovered()).isTrue();
		assertThat(result.caseResults()).hasSize(3);
	}

	private String fizzBuzzDone(int i) {
		String result = "";
		// if (i == 3 || i == 6) {
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

	@Example
	void nextGenPbtExample() {
		TDD.forAll(Numbers.integers().between(1, 1_000_000))
		   .publisher(PlatformPublisher.STDOUT_PLAIN)
		   .verifyCase(
			   "normal numbers",
			   number -> true,
			   number -> {
				   var result = fizzBuzz(number);
				   assertThat(result).isEqualTo(Integer.toString(number));
			   }
		   )
		   .drive();
	}

	String fizzBuzz(int number) {
		return "" + number;
	}
}