package jqwik2;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

@Group
class JqwikPropertyTests {

	@Group
	class Building {

		@Example
		void buildPropertyWithId() throws Throwable {
			JqwikProperty.Builder builder = JqwikProperty.property("myId");
			JqwikProperty.Verifier1<Integer> verifier = builder.forAll(Numbers.integers());
			JqwikProperty property = verifier.check(i -> i == 42);

			assertThat(property.id()).isEqualTo("myId");
			assertThat(property.arity()).isEqualTo(1);
			assertThat(property.arbitraries()).containsExactly(Numbers.integers());

			var condition = property.condition();
			assertThat(condition.check(List.of(42))).isTrue();
			assertThat(condition.check(List.of(41))).isFalse();
		}

		@Example
		void buildWithDefaultId() {
			JqwikProperty property = JqwikProperty.forAll(Strings.strings()).check(s -> s.length() > 1);

			String expectedDefaultId = getClass().getName() + "#" + "buildWithDefaultId";

			assertThat(property.id()).isEqualTo(expectedDefaultId);
			assertThat(property.arity()).isEqualTo(1);
			assertThat(property.arbitraries()).containsExactly(Strings.strings());
		}
	}
}
