package jqwik2;

import java.util.stream.*;

import org.approvaltests.core.*;

public class TrimWhitespaceScrubber implements Scrubber {
	@Override
	public String scrub(String input) {
		return input.lines()
					.map(String::stripTrailing)
					.collect(Collectors.joining(System.lineSeparator()));
	}
}
