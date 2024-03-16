package jqwik2.api;

public interface Publisher {

	Publisher NULL = text -> {};

	Publisher STDOUT = new Publisher() {
		@Override
		public void report(String text) {
			System.out.print(text);
		}

		@Override
		public void reportLine(String text) {
			System.out.println(text);
		}

		@Override
		public boolean supportsAnsiCodes() {
			return true;
		}
	};

	void report(String text);

	default void reportLine(String text) {
		report(text + System.lineSeparator());
	}

	default boolean supportsAnsiCodes() {
		return false;
	}
}
