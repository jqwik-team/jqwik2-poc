package jqwik2.api;

public interface Publisher {

	void report(String text);

	default void reportLine(String text) {
		report(text + System.lineSeparator());
	}

	default boolean supportsAnsiCodes() {
		return false;
	}
}
