package jqwik2.tdd;

import java.util.*;

import jqwik2.api.validation.*;

public record TddResult(Status status, List<PropertyValidationResult> caseResults) {
	public enum Status {
		SUCCESSFUL,
		FAILED,
		ABORTED,
		NOT_COVERED
	}
}
