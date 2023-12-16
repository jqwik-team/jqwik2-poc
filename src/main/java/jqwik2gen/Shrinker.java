package jqwik2gen;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

public class Shrinker {
	private final List<Shrinkable<?>> startingShrinkables;
	private final Function<List<Object>, PropertyExecutionResult> property;
	private final SortedSet<List<Shrinkable<?>>> candidates = new TreeSet<>(parametersComparator());
	private List<Shrinkable<?>> favourite;

	private static Comparator<List<Shrinkable<?>>> parametersComparator() {
		return Comparator.comparing((List<Shrinkable<?>> shrinkables) -> shrinkables.getFirst().recording());
	}

	public Shrinker(List<Shrinkable<?>> startingShrinkables, Function<List<Object>, PropertyExecutionResult> property) {
		this.startingShrinkables = startingShrinkables;
		this.property = property;
		if (startingShrinkables.size() != 1) {
			throw new IllegalArgumentException("Only one parameter supported for now!");
		}
		candidates.add(startingShrinkables);
		favourite = startingShrinkables;
	}

	public Optional<List<Shrinkable<?>>> nextStep() {
		System.out.println("candidates: " + candidates);

		while (!candidates.isEmpty()) {
			List<Shrinkable<?>> nextCandidate = candidates.removeFirst();

			AtomicBoolean found = new AtomicBoolean(false);
			shrink(nextCandidate)
					.filter(shrinkables -> property.apply(values(shrinkables)) == PropertyExecutionResult.FAILED)
					.forEach(e -> {
						if (!favourite.equals(e)) {
							candidates.add(e);
							found.set(true);
						}
					});

			// TODO: Only set if first candidate is smaller than favourite
			if (found.get()) {
				favourite = candidates.first();
				return Optional.of(favourite);
			}
		}
		return Optional.empty();
	}

	private Stream<List<Shrinkable<?>>> shrink(List<Shrinkable<?>> nextCandidate) {
		Shrinkable<?> firstParam = nextCandidate.getFirst();
		return firstParam.shrink().map(List::of);
	}

	private List<Object> values(List<Shrinkable<?>> shrinkables) {
		return shrinkables.stream()
						  .map((Shrinkable<?> shrinkable) -> {
							  return (Object) shrinkable.value();
						  })
						  .toList();
	}
}
