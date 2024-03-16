package jqwik2;

import jqwik2.api.*;

public class StringPublisher implements Publisher {

	private StringBuilder report = new StringBuilder();

	@Override
	public void report(String text) {
		report.append(text);
	}

	public String contents() {
		return report.toString();
	}
}
