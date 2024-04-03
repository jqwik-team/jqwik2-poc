package jqwik2.tdd;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;
import jqwik2.tdd.TddProperty.*;

class TddP1<T1> extends AbstractTddProperty<P1<T1>> implements P1<T1> {
	private final Arbitrary<T1> a1;

	TddP1(String id, Arbitrary<T1> a1) {
		super(id);
		this.a1 = a1;
	}

	@Override
	public P1<T1> verifyCase(String label, Check.C1<T1> check1, Verify.V1<T1> v1) {
		var property = PropertyDescription.property(testCaseId(label))
										  .forAll(a1)
										  .verify(verifier(v1, check1));
		addTestCase(label, property);
		return this;
	}

	private Verify.V1<T1> verifier(Verify.V1<T1> v1, Check.C1<T1> check) {
		return t1 -> {
			Assume.that(check.check(t1));
			v1.verify(t1);
		};
	}

	@Override
	protected PropertyDescription everythingCoveredProperty(String everythingCoveredId, List<Condition> allCaseConditions) {
		Predicate<T1> everythingCovered = t1 -> allCaseConditions.stream().anyMatch(c -> {
			try {
				return c.check(List.of(t1));
			} catch (Throwable e) {
				return false;
			}
		});
		return PropertyDescription.property(everythingCoveredId)
								  .forAll(a1)
								  .check(everythingCovered::test);
	}

}
