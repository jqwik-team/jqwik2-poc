package jqwik2.internal.arbitraries;

import java.util.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.arbitraries.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;

public class DefaultStringArbitrary implements StringArbitrary {

	static final Pair<Integer, Arbitrary<? extends Integer>> WHITESPACE_CHARS;

	static {
		// determine WHITESPACE_CHARS at runtime because the environments differ . . .
		var whitespace =
			IntStream.range(Character.MIN_VALUE, Character.MAX_VALUE + 1)
					 .filter(Character::isWhitespace).boxed().collect(Collectors.toSet());
		WHITESPACE_CHARS = Pair.of(whitespace.size(), Values.of(whitespace));
	}

	private static Generator<Integer> defaultUnicodes() {
		// Per default only characters in Multilingual Plane 0 are generated and non-characters and private use characters are excluded
		return BaseGenerators.integers(Character.MIN_VALUE, Character.MAX_VALUE)
							 .filter(c -> !isNonCharacter(c) && !isPrivateUseCharacter(c));
	}

	private static boolean isNonCharacter(int codepoint) {
		if (codepoint >= 0xfdd0 && codepoint <= 0xfdef)
			return true;
		// see https://en.wikipedia.org/wiki/UTF-16#U+D800_to_U+DFFF
		if (codepoint >= 0xd800 && codepoint <= 0xdfff)
			return true;
		return codepoint == 0xfffe || codepoint == 0xffff;
	}

	private static boolean isPrivateUseCharacter(int codepoint) {
		return codepoint >= 0xe000 && codepoint <= 0xf8ff;
	}

	private final int minLength;
	private final int maxLength;

	private final Set<Pair<Integer, Arbitrary<? extends Integer>>> unicodeFrequencies;

	public DefaultStringArbitrary() {
		this(Set.of(), 0, BaseGenerators.DEFAULT_COLLECTION_SIZE);
	}

	private DefaultStringArbitrary(Set<Pair<Integer, Arbitrary<? extends Integer>>> unicodeFrequencies, int minLength, int maxLength) {
		this.unicodeFrequencies = unicodeFrequencies;
		if (minLength < 0) {
			throw new IllegalArgumentException("minLength must be >= 0");
		}
		if (maxLength < 0) {
			throw new IllegalArgumentException("maxLength must be >= 0");
		}
		if (maxLength < minLength) {
			throw new IllegalArgumentException("maxLength must be >= minLength");
		}
		this.minLength = minLength;
		this.maxLength = maxLength;
	}

	@Override
	public Generator<String> generator() {
		return BaseGenerators.strings(unicodes(), minLength, maxLength);
	}

	private Generator<Integer> unicodes() {
		if (unicodeFrequencies.isEmpty()) {
			return defaultUnicodes();
		}
		return Values.frequencyOf(unicodeFrequencies).generator();
	}

	/**
	 * Set the maximum allowed length {@code nexMaxLength} (included) of generated strings.
	 * This will also set the min length of the string if {@code newMaxLength} is smaller than the current min length.
	 *
	 * @param nexMaxLength
	 * @throws IllegalArgumentException if nexMaxLength &lt; 0
	 */
	@Override
	public StringArbitrary ofMaxLength(int nexMaxLength) {
		return new DefaultStringArbitrary(unicodeFrequencies, Math.min(minLength, nexMaxLength), nexMaxLength);
	}

	/**
	 * Set the minimum allowed length {@code newMinLength} (included) of generated strings.
	 * This will also set the max length of the string if {@code newMinLength} is larger than the current max length.
	 *
	 * @param newMinLength
	 * @throws IllegalArgumentException if newMinLength &lt; 0
	 */
	@Override
	public StringArbitrary ofMinLength(int newMinLength) {
		return new DefaultStringArbitrary(unicodeFrequencies, newMinLength, Math.max(maxLength, newMinLength));
	}

	/**
	 * Allow all ascii chars to show up in generated strings.
	 * <p>
	 * Can be combined with other methods that allow chars.
	 */
	@Override
	public StringArbitrary ascii() {
		return withCodepoints(0, MAX_ASCII_CODEPOINT);
	}

	/**
	 * Allow all alpha chars to show up in generated strings.
	 * <p>
	 * Can be combined with other methods that allow chars.
	 */
	@Override
	public StringArbitrary alpha() {
		return withCodepoints('a', 'z').withCodepoints('A', 'Z');
	}

	/**
	 * Allow all numeric chars (digits) to show up in generated strings.
	 * <p>
	 * Can be combined with other methods that allow chars.
	 */
	@Override
	public StringArbitrary numeric() {
		return withCodepoints('0', '9');
	}

	/**
	 * Allow all chars that will return {@code true} for
	 * {@link Character#isWhitespace(char)}.
	 * <p>
	 * Can be combined with other methods that allow chars.
	 */
	@Override
	public StringArbitrary whitespace() {
		return with(WHITESPACE_CHARS);
	}

	private DefaultStringArbitrary withCodepoints(int minCodepoint, int maxCodepoint) {
		if (minCodepoint < 0 || maxCodepoint < 0 || minCodepoint > maxCodepoint) {
			throw new IllegalArgumentException("minCodepoint and maxCodepoint must be >= 0 and minCodepoint <= maxCodepoint");
		}
		return with(Pair.of(maxCodepoint - minCodepoint, Numbers.integers().between(minCodepoint, maxCodepoint)));
	}

	private DefaultStringArbitrary with(Pair<Integer, Arbitrary<? extends Integer>> frequency) {
		var newFrequencies = new LinkedHashSet<>(unicodeFrequencies);
		newFrequencies.add(frequency);
		return new DefaultStringArbitrary(newFrequencies, minLength, maxLength);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		DefaultStringArbitrary that = (DefaultStringArbitrary) o;

		if (minLength != that.minLength) return false;
		if (maxLength != that.maxLength) return false;
		return unicodeFrequencies.equals(that.unicodeFrequencies);
	}

	@Override
	public int hashCode() {
		return Objects.hash(minLength, maxLength, unicodeFrequencies);
	}
}
