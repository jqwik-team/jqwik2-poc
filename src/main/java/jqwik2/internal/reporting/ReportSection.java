package jqwik2.internal.reporting;

import java.util.*;

import jqwik2.api.*;

public class ReportSection {

	private final Map<String, Object> entries = new LinkedHashMap<>();
	private final String header;

	public ReportSection(String header) {
		this.header = header;
	}

	public void append(String key, Object value) {
		entries.put(key, value);
	}

	public void publish(StringBuilder publisher) {
		// TODO: Publish nicely formatted according to example-report.txt
		if (entries.isEmpty()) {
			return;
		}

		publisher.append("%n|--%s--|%n".formatted(header));
		entries.forEach((key, value) -> {
			publisher.append("  %s | %s%n".formatted(key, value));
		});
	}

	public void publish(String key, PlatformPublisher platformPublisher) {
		var publisher = new StringBuilder();
		publish(publisher);
		platformPublisher.publish(key, publisher.toString());
	}
}
