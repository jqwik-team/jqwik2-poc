package jqwik2.internal.reporting;

class ReportingSupport {
	static String repeat(char c, int times) {
		return String.valueOf(c).repeat(times);
	}

	static String padRight(String aString, int padLength) {
		return aString + " ".repeat(padLength - aString.length());
	}
}
