package jqwik2.api;

import java.util.*;

import jqwik2.api.recording.*;

public interface ExhaustiveGenerator extends Iterable<Recording> {

	Optional<Long> maxCount();
}

