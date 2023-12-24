package jqwik2;

import java.util.*;

public record PropertyRun(PropertyExecutionResult result, List<FalsifiedSample> failures) {
}
