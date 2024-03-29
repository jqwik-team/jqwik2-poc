package jqwik2.api.description;

import java.util.*;

import jqwik2.api.functions.*;

public interface Classifier {

	static <T1> Case<Check.C1<T1>> caseOf(Check.C1<T1> classify, String label, double minPercentage) {
		return new Case<>(classify.asCondition(), label, minPercentage);
	}

	static <T1> Case<Check.C1<T1>> caseOf(Check.C1<T1> classify, String label) {
		return caseOf(classify, label, 0.0);
	}

	static <T1, T2> Case<Check.C2<T1, T2>> caseOf(Check.C2<T1, T2> classify, String label, double minPercentage) {
		return new Case<>(classify.asCondition(), label, minPercentage);
	}

	static <T1, T2> Case<Check.C2<T1, T2>> caseOf(Check.C2<T1, T2> classify, String label) {
		return caseOf(classify, label, 0.0);
	}

	List<Case<?>> cases();

	record Case<C extends Check<C>>(Condition condition, String label, double minPercentage) {
		public Case(Condition condition, String label, double minPercentage) {
			if (label.isBlank()) {
				throw new IllegalArgumentException("Label must not be blank");
			}
			this.condition = condition;
			this.label = label.trim();
			this.minPercentage = minPercentage;
		}
	}
}
