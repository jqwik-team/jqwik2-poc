package jqwik2;

import java.util.*;

public record PropertyRun(ExecutionResult result, List<FalsifiedSample> failures) {
}
