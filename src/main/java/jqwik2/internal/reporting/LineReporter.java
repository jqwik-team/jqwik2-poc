package jqwik2.internal.reporting;

import java.util.function.*;

public class LineReporter {

	private final Consumer<String> builder;
	private final int baseIndent;

	public LineReporter(Consumer<String> builder) {
		this(builder, 0);
	}

	public LineReporter(Consumer<String> builder, int baseIndent) {
		this.builder = builder;
		this.baseIndent = baseIndent;
	}

	private String multiply(char c, int times) {
		StringBuilder builder = new StringBuilder();
		for (int j = 0; j < times; j++) {
			builder.append(c);
		}
		return builder.toString();
	}

	public void appendUnderline(int indentLevel, int length) {
		String underline = multiply('-', length);
		appendLn(indentLevel, underline);
	}

	public void appendLn(int indentLevel, String line) {
		int effectiveIndent = indentLevel + baseIndent;
		String indentation = multiply(' ', effectiveIndent * 2);
		builder.accept(String.format("%s%s%n", indentation, line));
	}

}
