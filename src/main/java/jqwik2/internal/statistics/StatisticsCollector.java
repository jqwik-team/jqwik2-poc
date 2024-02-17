package jqwik2.internal.statistics;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.statistics.*;

public class StatisticsCollector {

	private final Map<List<Object>, Integer> counts = new ConcurrentHashMap<>();
	private final String label;

	public StatisticsCollector(String label) {
		this.label = label;
	}

	private void collect(List<Object> values) {
		int count = counts.computeIfAbsent(values, k -> 0);
		counts.put(values, count + 1);
	}

	public <T1> Statistics.Collector.C1<T1> forTypes(Class<T1> type) {
		return new C1Impl();
	}

	public void coverage(Consumer<Statistics.Checker> coverage) {

	}

	private class C1Impl<T1> implements Statistics.Collector.C1<T1> {

		private List<T1> values = null;

		@Override
		public void collect(T1 v1) {
			this.values = null;
			StatisticsCollector.this.collect(List.of(v1));
		}

		@Override
		public List<T1> values() {
			if (values == null) {
				values = computeValues();
			}
			return values;
		}

		@SuppressWarnings("unchecked")
		private List<T1> computeValues() {
			return counts.entrySet().stream()
						   .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
						   .map(entry -> (T1) entry.getKey().getFirst())
						   .collect(Collectors.toList());

		}

		@Override
		public int count(T1 v1) {
			return counts.getOrDefault(List.of(v1), 0);
		}
	}
}
