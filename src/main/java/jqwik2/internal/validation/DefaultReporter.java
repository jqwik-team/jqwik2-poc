package jqwik2.internal.validation;

import java.util.*;

import jqwik2.api.*;
import jqwik2.internal.*;

public class DefaultReporter implements Reporter {
	private final Map<String, List<Pair<String, Object>>> report = new LinkedHashMap<>();
	private final Publisher platformPublisher;

	DefaultReporter(Publisher platformPublisher) {
		this.platformPublisher = platformPublisher;
		List.of(Reporter.CATEGORY_RESULT, Reporter.CATEGORY_PARAMETER)
				.forEach(category -> report.put(category, new ArrayList<>()));
	}

	@Override
	public void appendToReport(String category, String key, Object value) {
		report.computeIfAbsent(category, k -> new ArrayList<>()).add(Pair.of(key, value));
	}

	public void publishReport() {
		report.forEach((category, pairs) -> {
			if (pairs.isEmpty()) {
				return;
			}
			platformPublisher.reportLine("");

			// TODO: Publish nicely formatted according to example-report.txt
			platformPublisher.reportLine("|--" + category + "--|");
			for (Pair<String, Object> keyValue : pairs) {
				platformPublisher.reportLine("  " + keyValue.first() + " | " + keyValue.second());
			}
		});
	}
}
