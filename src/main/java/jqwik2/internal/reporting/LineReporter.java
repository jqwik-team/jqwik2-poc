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

	public void appendUnderline(int indentLevel, int length) {
		String underline = ReportingSupport.repeat('-', length);
		appendLn(indentLevel, underline);
	}

	public void appendLn(int indentLevel, String line) {
		int effectiveIndent = indentLevel + baseIndent;
		String indentation = ReportingSupport.repeat(' ', effectiveIndent * 2);
		builder.accept(String.format("%s%s%n", indentation, line));
	}

}
