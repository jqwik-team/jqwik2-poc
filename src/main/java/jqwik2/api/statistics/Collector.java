package jqwik2.api.statistics;

import java.util.*;
import java.util.function.*;

import jqwik2.internal.*;
import jqwik2.internal.statistics.*;

public interface Collector {

	static Collector.C1<Object> create(String label) {
		return create(label, Object.class);
	}

	static <T1> Collector.C1<T1> create(String label, Class<T1> valueType) {
		return new StatisticsCollector(label).forTypes(valueType);
	}

	interface C1<T1> extends Collector {
		/**
		 * Call this method to record a single value of type {@code T1}
		 * for statistical data about generated values.
		 */
		void collect(T1 v1);

		int count(T1 v1);

		/**
		 * Returns all distinct values of type {@code T1} that have been collected.
		 * Sorted by frequency of occurrence in descending order.
		 */
		List<T1> values();
	}

	interface C2<T1, T2> extends Collector {
		/**
		 * Call this method to record two values of type {@code T1} and {@code T2}
		 * for statistical data about generated values.
		 */
		void collect(T1 v1, T2 v2);

		int count(T1 v1, T2 v2);
	}
}
