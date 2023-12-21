package jqwik2;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

public class Shrinker {
	private final Function<List<Object>, PropertyExecutionResult> property;
	private final SortedSet<Sample> candidates = new TreeSet<>();
	private Sample best;

	public Shrinker(Sample sample, Function<List<Object>, PropertyExecutionResult> property) {
		if (sample.size() != 1) {
			throw new IllegalArgumentException("Only one parameter supported for now!");
		}
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
			shrinkSample(nextCandidate)
				.filter(sample -> property.apply(sample.values()) == PropertyExecutionResult.FAILED)
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

	private Stream<Sample> shrinkSample(Sample nextCandidate) {
		Shrinkable<Object> firstParam = nextCandidate.shrinkables().getFirst();
		return firstParam.shrink().map((Shrinkable<Object> s) -> new Sample(List.of(s)));
	}

}
