package jqwik2;

import java.util.*;

import jqwik2.api.*;

public class StringPublisher implements PlatformPublisher {

	private StringBuilder report = new StringBuilder();
	private final boolean supportsAnsiCodes;

	public StringPublisher() {
		this(false);
	}

	public StringPublisher(boolean supportsAnsiCodes) {
		this.supportsAnsiCodes = supportsAnsiCodes;
	}

	public String contents() {
		return report.toString();
	}

	@Override
	public void publish(String key, Map<String, String> entry) {
		List<String> values = new ArrayList<>();
		for (Map.Entry<String, String> keyValue : entry.entrySet()) {
			values.add("%s = %s".formatted(keyValue.getKey(), keyValue.getValue()));
		}
		report.append(String.join(", ", values)).append(System.lineSeparator());
	}

	@Override
	public boolean supportsAnsiCodes() {
		return supportsAnsiCodes;
	}
}
