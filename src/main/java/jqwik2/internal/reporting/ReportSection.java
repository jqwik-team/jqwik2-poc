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
		if (entries.isEmpty()) {
			return;
		}

		int maxKeyLength = entries.keySet().stream().mapToInt(String::length).max().orElse(0);
		int maxValueLength = entries.values().stream().mapToInt(value -> value.toString().length()).max().orElse(0);
		int lineLength = maxKeyLength + maxValueLength + 7;
		int numberOfDashes = Math.max(2, lineLength - header.length() - 2) / 2;
		String dashes = ReportingSupport.repeat('-', numberOfDashes);

		publisher.append("%n|%s%s%s|%n".formatted(dashes, header, dashes));
		entries.forEach((key, value) -> {
			var paddedKey = ReportingSupport.padRight(key, maxKeyLength);
			publisher.append("  %s | %s%n".formatted(paddedKey, value));
		});
	}

	public void publish(String key, PlatformPublisher platformPublisher) {
		var publisher = new StringBuilder();
		publish(publisher);
		platformPublisher.publish(key, publisher.toString());
	}
}
