package jqwik2;

import java.util.*;

import jqwik2.api.Arbitrary;
import jqwik2.api.arbitraries.*;
import jqwik2.api.description.*;
import org.assertj.core.api.*;
import org.opentest4j.*;

import net.jqwik.api.*;

import static jqwik2.api.description.Classifier.*;
import static org.assertj.core.api.Assertions.*;

class PropertyDescriptionTests {

	@Example
	void buildPropertyWithId() throws Throwable {
		PropertyDescription.Builder builder = PropertyDescription.property("myId");
		PropertyDescription.Verifier1<Integer> verifier = builder.forAll(Numbers.integers());
		PropertyDescription property = verifier.check(i -> i == 42);

		assertThat(property.id()).isEqualTo("myId");
		assertThat(property.arity()).isEqualTo(1);
		assertThat(property.arbitraries()).containsExactly(Numbers.integers());

		var condition = property.condition();
		assertThat(condition.check(List.of(42))).isTrue();
		assertThat(condition.check(List.of(41))).isFalse();
	}

	@Example
	void buildPropertyWithVerification() throws Throwable {
		PropertyDescription property =
			PropertyDescription.property("myIdVerify")
							   .forAll(Numbers.integers())
							   .verify(i -> Assertions.assertThat(i).isEqualTo(42));

		assertThat(property.arity()).isEqualTo(1);
		assertThat(property.arbitraries()).containsExactly(Numbers.integers());

		var condition = property.condition();
		assertThat(condition.check(List.of(42))).isTrue();
		assertThatThrownBy(
			() -> condition.check(List.of(41))
		).isInstanceOf(AssertionFailedError.class);
	}

	@Example
	void buildWithDefaultId() {
		Arbitrary<String> arbitrary = Strings.strings();
		PropertyDescription property = PropertyDescription.property().forAll(arbitrary).check(s -> s.length() > 1);

		String expectedDefaultId = getClass().getName() + "#" + "buildWithDefaultId";

		assertThat(property.id()).isEqualTo(expectedDefaultId);
		assertThat(property.arity()).isEqualTo(1);
		assertThat(property.arbitraries()).containsExactly(Strings.strings());
	}

	@Example
	void buildPropertyWithTwoParameters() throws Throwable {
		PropertyDescription.Builder builder = PropertyDescription.property("myId2");
		PropertyDescription.Verifier2<Integer, String> verifier = builder.forAll(
			Numbers.integers(),
			Strings.strings()
		);
		PropertyDescription property = verifier.check((i, s) -> s.length() == i);

		assertThat(property.arity()).isEqualTo(2);
		assertThat(property.arbitraries()).containsExactly(Numbers.integers(), Strings.strings());

		var condition = property.condition();
		assertThat(condition.check(List.of(3, "abc"))).isTrue();
		assertThat(condition.check(List.of(4, "abc"))).isFalse();
	}

	@Example
	void buildPropertyWithClassifier() throws Throwable {
		PropertyDescription property =
			PropertyDescription.property("myId")
							   .forAll(Numbers.integers())
							   .classify(List.of(
								   caseOf(i -> i > 0, "positive", 40.0),
								   caseOf(i -> i < 0, "negative", 40.0),
								   caseOf(i -> i == 0, "zero")
							   ))
							   .check(i -> i == 42);

		assertThat(property.arity()).isEqualTo(1);

		var classifiers = property.classifiers();
		assertThat(classifiers).hasSize(1);
		var firstClassifier = classifiers.getFirst();
		assertThat(firstClassifier.cases()).hasSize(3);

		var labels = firstClassifier.cases().stream().map(Case::label).toList();
		assertThat(labels).containsExactlyInAnyOrder("positive", "negative", "zero");
		var minPercentages = firstClassifier.cases().stream().map(Case::minPercentage).toList();
		assertThat(minPercentages).containsExactlyInAnyOrder(40.0, 40.0, 0.0);
	}

	@Example
	void buildPropertyWithTwoClassifiers() throws Throwable {
		PropertyDescription property =
			PropertyDescription.property("myId")
							   .forAll(Numbers.integers())
							   .classify(List.of(
								   caseOf(i -> i > 0, "positive", 40.0)
							   ))
							   .classify(List.of(
								   caseOf(i -> i % 2 == 0, "even", 40.0)
							   ))
							   .check(i -> i == 42);

		assertThat(property.arity()).isEqualTo(1);

		var classifiers = property.classifiers();
		assertThat(classifiers).hasSize(2);

		assertThat(classifiers.getFirst().cases().getFirst().label()).isEqualTo("positive");
		assertThat(classifiers.get(1).cases().getFirst().label()).isEqualTo("even");
	}

	@Example
	void buildPropertyWith2ParamsAndClassifier() throws Throwable {
		PropertyDescription property =
			PropertyDescription.property("myId")
							   .forAll(Numbers.integers(), Strings.strings())
							   .classify(List.of(
								   caseOf((i, s) -> i > 0, "positive", 40.0),
								   caseOf((i, s) -> i < 0, "negative", 40.0),
								   caseOf((i, s) -> i == 0, "zero")
							   ))
							   .check((i, s) -> i == s.length());

		assertThat(property.arity()).isEqualTo(2);

		var classifiers = property.classifiers();
		assertThat(classifiers).hasSize(1);
		var firstClassifier = classifiers.getFirst();
		assertThat(firstClassifier.cases()).hasSize(3);

		var labels = firstClassifier.cases().stream().map(Case::label).toList();
		assertThat(labels).containsExactlyInAnyOrder("positive", "negative", "zero");
		var minPercentages = firstClassifier.cases().stream().map(Case::minPercentage).toList();
		assertThat(minPercentages).containsExactlyInAnyOrder(40.0, 40.0, 0.0);
	}

}
