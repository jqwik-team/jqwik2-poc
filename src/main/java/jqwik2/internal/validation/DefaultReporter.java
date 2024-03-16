package jqwik2.internal.validation;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class DefaultReporter implements Reporter {
	private final Map<String, List<Pair<String, Object>>> report = new LinkedHashMap<>();

	DefaultReporter() {
		List.of(Reporter.CATEGORY_RESULT, Reporter.CATEGORY_PARAMETER)
				.forEach(category -> report.put(category, new ArrayList<>()));
	}

	@Override
	public void appendToReport(String category, String key, Object value) {
		report.computeIfAbsent(category, k -> new ArrayList<>()).add(Pair.of(key, value));
	}

	public void publishReport(Publisher publisher) {
		report.forEach((category, pairs) -> {
			if (pairs.isEmpty()) {
				return;
			}
			publisher.reportLine("");

			// TODO: Publish nicely formatted according to example-report.txt
			publisher.reportLine("|--" + category + "--|");
			for (Pair<String, Object> keyValue : pairs) {
				publisher.reportLine("  " + keyValue.first() + " | " + keyValue.second());
			}
		});
	}
}
