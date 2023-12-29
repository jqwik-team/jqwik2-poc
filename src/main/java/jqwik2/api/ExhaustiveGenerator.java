package jqwik2.api;

import java.util.*;

public interface ExhaustiveGenerator extends IterableGenSource {

	Optional<Long> maxCount();
}

