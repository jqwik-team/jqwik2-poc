package jqwik2.api;

import java.time.*;
import java.util.*;

public interface PlatformPublisher {

	PlatformPublisher NULL = (key, entry) -> {};

	PlatformPublisher STDOUT = new PlatformPublisher() {

		@Override
		public void publish(String key, Map<String, String> reportEntry) {
			LocalDateTime now = LocalDateTime.now();
			List<String> values = new ArrayList<>();
			values.add("timestamp = %s".formatted(now));
			for (Map.Entry<String, String> entry : reportEntry.entrySet()) {
				if (entry.getKey() == null || entry.getKey().isBlank()) {
					throw new IllegalArgumentException("Key must not be null or blank");
				}
				values.add("%s = %s".formatted(entry.getKey(), entry.getValue()));
			}
			System.out.println(String.join(", ", values));
		}

		@Override
		public boolean supportsAnsiCodes() {
			return true;
		}
	};

	PlatformPublisher STDOUT_PLAIN = new PlatformPublisher() {

		@Override
		public void publish(String key, Map<String, String> reportEntry) {
			List<String> values = new ArrayList<>();
			for (Map.Entry<String, String> entry : reportEntry.entrySet()) {
				if (entry.getKey() == null || entry.getKey().isBlank()) {
					throw new IllegalArgumentException("Key must not be null or blank");
				}
				values.add("%s = %s".formatted(entry.getKey(), entry.getValue()));
			}
			System.out.println(String.join(", ", values));
		}

		@Override
		public boolean supportsAnsiCodes() {
			return true;
		}
	};

	default void publish(String key, String value) {
		publish(key, Map.of(key, value));
	}

	void publish(String key, Map<String, String> entry);

	default boolean supportsAnsiCodes() {
		return false;
	}
}
