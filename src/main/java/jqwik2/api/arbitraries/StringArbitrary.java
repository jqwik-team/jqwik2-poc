package jqwik2.api.arbitraries;

import jqwik2.api.*;

public interface StringArbitrary extends Arbitrary<String> {

	int MAX_ASCII_CODEPOINT = 0x007F;

	/**
	 * Set the maximum allowed length {@code maxLength} (included) of generated strings.
	 *
	 * @throws IllegalArgumentException if maxLength &lt; 0 or maxLength &lt; min length
	 */
	StringArbitrary ofMaxLength(int maxLength);

	/**
	 * Set the minimum allowed length {@code minLength} (included) of generated strings.
	 * This will also set the max length of the string if {@code minLength} is larger than the current max length.
	 *
	 * @throws IllegalArgumentException if minLength &lt; 0
	 */
	StringArbitrary ofMinLength(int minLength);

	/**
	 * Fix the length to {@code length} of generated strings.
	 *
	 * @throws IllegalArgumentException if length &lt; 0
	 */
	default StringArbitrary ofLength(int length) {
		return ofMinLength(length).ofMaxLength(length);
	}
	/**
	 * Allow all ascii chars to show up in generated strings.
	 *
	 * Can be combined with other methods that allow chars.
	 */
	StringArbitrary ascii();

	/**
	 * Allow all alpha chars to show up in generated strings.
	 *
	 * Can be combined with other methods that allow chars.
	 */
	StringArbitrary alpha();

	/**
	 * Allow all numeric chars (digits) to show up in generated strings.
	 *
	 * Can be combined with other methods that allow chars.
	 */
	StringArbitrary numeric();

	/**
	 * Allow all chars that will return {@code true} for
	 * {@link Character#isWhitespace(char)}.
	 *
	 * Can be combined with other methods that allow chars.
	 */
	StringArbitrary whitespace();

}
