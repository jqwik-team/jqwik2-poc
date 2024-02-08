package jqwik2;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.internal.*;
import org.assertj.core.api.*;

import net.jqwik.api.*;

class GrowingCountersTests {
	// @Example
	void experiment() {
		Supplier<Stream<Integer>> c1 = () -> IntStream.range(0, 100).boxed();
		Supplier<Stream<Integer>> c2 = () -> IntStream.range(0, 2000).boxed();
		Supplier<Stream<Integer>> c3 = () -> IntStream.range(0, 9999).boxed();
		Supplier<Stream<Integer>> c4 = () -> IntStream.range(0, 4500).boxed();

		var suppliers = List.of(c1, c2, c3, c4);

		var counters = c1.get().flatMap(v1 -> {
			return c2.get().flatMap(v2 -> {
				return c3.get().flatMap(v3 -> {
					return c4.get().map(v4 -> List.of(v1, v2, v3, v4));
				});
			});
		});

		Iterator<List<Integer>> iterator = counters.limit(50).iterator();
		for (int i = 0; i < 50; i++) {
			System.out.println(iterator.next());
		}

		// counters.limit(50).forEach(System.out::println);
	}

	@Example
	void single() {
		GrowingCounters counters = new GrowingCounters(
			List.of(3)
		);

		AtomicInteger count = new AtomicInteger(0);
		counters.iterator(4).forEachRemaining(counter -> {
			count.incrementAndGet();
			// System.out.println(counter);
		});

		Assertions.assertThat(count.get()).isEqualTo(4);
	}

	@Example
	void twoEqualCounters() {
		GrowingCounters counters = new GrowingCounters(
			List.of(3, 3)
		);

		AtomicInteger count = new AtomicInteger(0);
		counters.iterator(5).forEachRemaining(counter -> {
			count.incrementAndGet();
			System.out.println(counter);
		});

		Assertions.assertThat(count.get()).isEqualTo(16);
	}

}

class GrowingCounters {

	private final List<Integer> counters;
	private final int maxValue;

	GrowingCounters(List<Integer> counters) {
		this.counters = counters;
		this.maxValue = counters.stream().mapToInt(i -> i).max().orElse(0);
	}

	public Iterator<List<Integer>> iterator(int maxDepth) {
		List<Stream<List<Integer>>> streams = streams(maxDepth);
		return StreamConcatenation.concat(streams).limit(1000).iterator();
	}

	private List<Stream<List<Integer>>> streams(int maxDepth) {
		return IntStream.range(0, maxDepth + 1)
						.takeWhile(i -> i <= maxValue)
						.mapToObj(this::streamForDepth)
						.collect(Collectors.toList());
	}

	private Stream<List<Integer>> streamForDepth(int depth) {
		Stream<List<Integer>> result = Stream.empty();
		List<Supplier<Stream<Integer>>> suppliers = streamSuppliers(depth);
		for (int i = suppliers.size() - 1; i >= 0; i--) {
			var supplier = suppliers.get(i);
			if (i >= suppliers.size() - 1) {
				result = supplier.get().map(List::of);
			} else {
				// TODO: flatMap to combine with previous result
				// result = result.flatMap(value -> supplier.get().flatMap(v -> {
				// 	var copy = new ArrayList<>(value);
				// 	copy.add(v);
				// 	return copy;
				// }));
			}
		}

		return result.filter(list -> list.stream().noneMatch(e -> e != depth));
	}

	private List<Supplier<Stream<Integer>>> streamSuppliers(int depth) {
		return counters.stream()
					   .map(c -> {
						   int max = Math.min(c, depth);
						   return (Supplier<Stream<Integer>>) () -> IntStream.range(0, max + 1).boxed();
					   })
					   .toList();
	}

}
