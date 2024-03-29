package jqwik2.tdd;

import java.util.*;

import jqwik2.api.validation.*;

public record TddResult(PropertyValidationStatus status, List<PropertyValidationResult> caseResults, boolean everythingCovered) {
}
