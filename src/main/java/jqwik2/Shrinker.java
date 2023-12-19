package jqwik2;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

public class Shrinker {
	private final Function<List<Object>, PropertyExecutionResult> property;
	private final SortedSet<List<Shrinkable<Object>>> candidates = new TreeSet<>(this::compare);
	private List<Shrinkable<Object>> favourite;

	public Shrinker(List<Shrinkable<Object>> startingShrinkables, Function<List<Object>, PropertyExecutionResult> property) {
		this.property = property;
		if (startingShrinkables.size() != 1) {
			throw new IllegalArgumentException("Only one parameter supported for now!");
		}
		candidates.add(startingShrinkables);
		favourite = startingShrinkables;
	}

	public Optional<List<Shrinkable<Object>>> nextStep() {
		while (!candidates.isEmpty()) {
			//System.out.println("candidates: " + candidates);
			List<Shrinkable<Object>> nextCandidate = candidates.removeFirst();

			AtomicBoolean found = new AtomicBoolean(false);
			shrink(nextCandidate)
				.filter(shrinkables -> property.apply(values(shrinkables)) == PropertyExecutionResult.FAILED)
				.filter(candidate -> compare(candidate, favourite) < 0)
				.forEach(e -> {
					if (!favourite.equals(e)) {
						candidates.add(e);
						found.set(true);
					}
				});

			if (candidates.isEmpty()) {
				break;
			}

			if (found.get()) {
				favourite = candidates.first();
				return Optional.of(favourite);
			}
		}
		return Optional.empty();
	}

	private int compare(List<Shrinkable<Object>> left, List<Shrinkable<Object>> right) {
		return left.getFirst().compareTo(right.getFirst());
	}

	private Stream<List<Shrinkable<Object>>> shrink(List<Shrinkable<Object>> nextCandidate) {
		Shrinkable<Object> firstParam = nextCandidate.getFirst();
		return firstParam.shrink().map(List::of);
	}

	private List<Object> values(List<Shrinkable<Object>> shrinkables) {
		return shrinkables.stream()
						  .map(Shrinkable::value)
						  .toList();
	}
}
