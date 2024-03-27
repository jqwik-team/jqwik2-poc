package jqwik2.tdd;

import jqwik2.api.arbitraries.*;
import jqwik2.api.description.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class TestDrivingTests {

	@Example
	void fizzBuzz() {
		var property =
			PropertyDescription.property()
							   .forAll(Numbers.integers().between(1, Integer.MAX_VALUE))
							   .verify(i -> {
								   var s = fizzBuzz(i);
								   if (i % 3 == 0) {
									   TddTry.label("Divisible by 3");
									   assertThat(s).startsWith("Fizz");
									   TddTry.done();
								   }
								   if (i % 5 == 0) {
									   TddTry.label("Divisible by 5");
									   assertThat(s).endsWith("Buzz");
									   TddTry.done();
								   }
								   if (i % 3 != 0 && i % 5 != 0) {
									   TddTry.label("Not divisible");
									   assertThat(s).isEqualTo(Integer.toString(i));
									   TddTry.done();
								   }
							   });

		TestDriver testDriver = TestDriver.forProperty(property);
		// .tddDatabase(new TddDatabase("./src/test/java/jqwik2/tdd/fizzBuzz.tdd"));
		testDriver.drive();
	}

	@Example
	// @Disabled("Not yet implemented")
	void fizzBuzz2() {
		var tddProperty =
			TddProperty.id("myId")
					   .forAll(Numbers.integers().between(1, Integer.MAX_VALUE));
		// .verifyCase("normal number", i -> true,
		// 		 i -> {
		// 			 var s = fizzBuzz(i);
		// 			 assertThat(s).equals(Integer.toString(i));
		// 		 }
		// );

		TddResult result = tddProperty.drive();
		assertThat(result.status()).isEqualTo(TddResult.Status.NOT_COVERED);
	}

	private String fizzBuzz(int i) {
		String result = "";
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
