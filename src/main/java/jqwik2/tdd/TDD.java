package jqwik2.tdd;

import java.util.*;

import jqwik2.api.description.*;
import org.opentest4j.*;

public class TDD {

	private static TDD current = null;

	static TDD current() {
		if (current == null) {
			throw new IllegalStateException("No TDD instance available");
		}
		return current;
	}

	public static TDD start(PropertyDescription property) {
		if (current != null) {
			throw new IllegalStateException("TDD cycle running already for property " + current.property.id());
		}
		current = new TDD(property);
		return current;
	}

	static void done() {
		current().covered();
	}

	static void label(String label) {
		current().useLabel(label);
	}

	private final PropertyDescription property;

	private boolean covered = false;
	private final Set<String> labels = new HashSet<>();

	public TDD(PropertyDescription property) {
		this.property = property;
	}

	private void covered() {
		this.covered = true;
	}

	private void useLabel(String label) {
		labels.add(label);
	}

	public void finish() {
		if (current != this) {
			throw new IllegalStateException("TDD instance not active");
		}
		current = null;
	}

	public void checkCovered() {
		if (!covered) {
			var message = "Sample not covered";
			throw new AssertionFailedError(message);
		}
	}

	public Set<String> labels() {
		return labels;
	}
}
