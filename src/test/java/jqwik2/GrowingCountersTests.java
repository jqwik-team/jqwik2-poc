package jqwik2;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.internal.*;
import org.assertj.core.api.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

/**
 * These tests are currently not related to the rest of jqwik2.
 * They serve to experiment with a new way to generate growing counters.
 */
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

		assertThat(count.get()).isEqualTo(4);
	}

	@Example
	void twoEqualCounters() {
		GrowingCounters counters = new GrowingCounters(
			List.of(3, 3)
		);

		AtomicInteger count = new AtomicInteger(0);
		counters.iterator(5).forEachRemaining(counter -> {
			count.incrementAndGet();
			// System.out.println(counter);
		});

		assertThat(count.get()).isEqualTo(16);
	}

	@Example
	void threeCounters() {
		GrowingCounters counters = new GrowingCounters(
			List.of(3, 2, 1000)
		);

		AtomicInteger count = new AtomicInteger(0);
		counters.iterator(10).forEachRemaining(counter -> {
			count.incrementAndGet();
			// System.out.println(counter);
		});

		assertThat(count.get()).isEqualTo(132);
	}

	@Example
	void manyLargeCounters() {
		GrowingCounters counters = new GrowingCounters(
			List.of(3000, 2000, 1000, 4000)
		);

		AtomicInteger count = new AtomicInteger(0);
		counters.iterator(9).forEachRemaining(counter -> {
			count.incrementAndGet();
			// System.out.println(counter);
		});

		assertThat(count.get()).isEqualTo(10000);
	}

}

/**
 * Current solution generates all possible combinations of counters for one target depth.
 * Then it filters out those that do not contain the target depth.
 *
 * Alternative algorithmic solution idea:
 * - Consider tuples of choices to be vectors of size n, where n is the sum of all counters
 * - Generate list with all values == 0,
 * - for next size of "vectors" add all vectors of size 1 and remember resulting set
 * - repeat until your target size has been reached
 * Advantages:
 * - It is independent of number of generators since choices can cross generator boundaries
 * - Values grow by size (not depth),
 * - easy to implement,
 * - could probably be done concurrently within same size
 * Disadvantage: Memory grows quadratically with each increasing size
 * Open questions:
 * - How to map structured sources onto vectors?
 * - How to handle changing dimensions, e.g. through oneOf generators?
 */
class GrowingCounters {

	private final List<Integer> counters;
	private final int maxValue;

	GrowingCounters(List<Integer> counters) {
		this.counters = counters;
		this.maxValue = counters.stream().mapToInt(i -> i).max().orElse(0);
	}

	public Iterator<List<Integer>> iterator(int maxDepth) {
		List<Stream<List<Integer>>> streams = streams(maxDepth);
		return StreamConcatenation.concat(streams).iterator();
	}

	private List<Stream<List<Integer>>> streams(int maxDepth) {
		return IntStream.range(0, maxDepth + 1)
						.takeWhile(i -> i <= maxValue)
						.mapToObj(this::streamForDepth)
						.collect(Collectors.toList());
	}

	private Stream<List<Integer>> streamForDepth(int depth) {
		Supplier<Stream<List<Integer>>>[] result = new Supplier[counters.size()];
		List<Supplier<Stream<Integer>>> suppliers = streamSuppliers(depth);
		for (int i = suppliers.size() - 1; i >= 0; i--) {
			var index = i; // Needed for lambda
			var supplier = suppliers.get(index);
			if (index >= suppliers.size() - 1) {
				result[index] = () -> supplier.get().map(List::of);
			} else {
				result[index] = () -> supplier.get().flatMap(
					v -> result[index + 1].get().map(list -> {
						var copy = new ArrayList<>(list);
						copy.addFirst(v);
						return copy;
					}));
			}
		}

		Stream<List<Integer>> stream = result[0].get();
		return stream.filter(list -> contains(list, depth));
	}

	private static boolean contains(List<Integer> list, int depth) {
		return list.contains(depth);
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
