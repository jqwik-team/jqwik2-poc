package jqwik2.api.description;

import java.util.*;

import jqwik2.api.description.PropertyDescription.*;

public interface Classifier {

	static <T1> Case<C1<T1>> caseOf(C1<T1> classify, String label, double minPercentage) {
		return new Case<>(classify.asCondition(), label, minPercentage);
	}

	static <T1> Case<C1<T1>> caseOf(C1<T1> classify, String label) {
		return caseOf(classify, label, 0.0);
	}

	static <T1, T2> Case<C2<T1, T2>> caseOf(C2<T1, T2> classify, String label, double minPercentage) {
		return new Case<>(classify.asCondition(), label, minPercentage);
	}

	static <T1, T2> Case<C2<T1, T2>> caseOf(C2<T1, T2> classify, String label) {
		return caseOf(classify, label, 0.0);
	}

	List<Case> cases();

	record Case<C extends PropertyDescription.Check<C>>(Condition condition, String label, double minPercentage) {
	}
}
