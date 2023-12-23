package jqwik2;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

public class Shrinker {
	private final Function<List<Object>, ExecutionResult> property;
	private final SortedSet<Sample> candidates = new TreeSet<>();
	private Sample best;

	public Shrinker(Sample sample, Function<List<Object>, ExecutionResult> property) {
		this.property = property;
		candidates.add(sample);
		best = sample;
	}

	public Sample best() {
		return best;
	}

	public Optional<Sample> next() {
		while (!candidates.isEmpty()) {
			//System.out.println("candidates: " + candidates);
			Sample nextCandidate = candidates.removeFirst();

			AtomicBoolean found = new AtomicBoolean(false);
			nextCandidate.shrink()
						 .filter(sample -> property.apply(sample.values()) == ExecutionResult.FAILED)
						 .filter(candidate -> candidate.compareTo(best) < 0)
						 .forEach(e -> {
							 if (!best.equals(e)) {
								 candidates.add(e);
								 found.set(true);
							 }
						 });

			if (candidates.isEmpty()) {
				break;
			}

			if (found.get()) {
				best = candidates.first();
				return Optional.of(best);
			}
		}
		return Optional.empty();
	}

}
