package jqwik2.tdd;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.description.*;
import jqwik2.api.functions.*;
import jqwik2.tdd.TddProperty.*;

class TddP2<T1, T2> extends AbstractTddProperty<P2<T1, T2>> implements P2<T1, T2> {
	private final Arbitrary<T1> a1;
	private final Arbitrary<T2> a2;

	TddP2(String id, Arbitrary<T1> a1, Arbitrary<T2> a2) {
		super(id);
		this.a1 = a1;
		this.a2 = a2;
	}

	@Override
	public P2<T1, T2> verifyCase(String label, Check.C2<T1, T2> check2, Verify.V2<T1, T2> v2) {
		var property = PropertyDescription.property(testCaseId(label))
										  .forAll(a1, a2)
										  .verify(verifier(v2, check2));
		addTestCase(label, property, check2.asCondition());
		return this;
	}

	private Verify.V2<T1, T2> verifier(Verify.V2<T1, T2> v2, Check.C2<T1, T2> check) {
		return (t1, t2) -> {
			Assume.that(check.check(t1, t2));
			v2.verify(t1, t2);
		};
	}

	@Override
	protected PropertyDescription everythingCoveredProperty(String everythingCoveredId, List<Condition> allCaseConditions) {
		Check.C2<T1, T2> everythingCovered = (t1, t2) -> allCaseConditions.stream().anyMatch(c -> {
			try {
				return c.check(List.of(t1, t2));
			} catch (Throwable e) {
				return false;
			}
		});
		return PropertyDescription.property(everythingCoveredId)
								  .forAll(a1, a2)
								  .check(everythingCovered::check);
	}

}
