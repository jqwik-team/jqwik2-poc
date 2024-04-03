package jqwik2.tdd;

import java.util.*;

import jqwik2.api.validation.*;

public record TddDrivingResult(PropertyValidationStatus status, List<PropertyValidationResult> caseResults, boolean everythingCovered) {
}
