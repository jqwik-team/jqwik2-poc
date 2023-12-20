package jqwik2;

import java.util.*;
import java.util.function.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

public class ShrinkingTests {

	@Example
	void generateWithUnsatisfyingGenSource() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);

		GenSource source = new RecordedSource(new AtomRecording(10, 0));
		Shrinkable<Integer> shrinkable = ints.generate_OLD(source);
		assertThat(shrinkable.value()).isEqualTo(10);

		assertThatThrownBy(() -> {
			RecordedSource recorded = new RecordedSource(new AtomRecording(100, 1));
			ints.generate_OLD(recorded);
		}).isInstanceOf(CannotGenerateException.class);

		assertThatThrownBy(() -> {
			RecordedSource recorded = new RecordedSource(new AtomRecording(100));
			ints.generate_OLD(recorded);
		}).isInstanceOf(CannotGenerateException.class);
	}

	@Example
	void shrinkIntegers() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);

		// -10
		GenSource source = new RecordedSource(new AtomRecording(10, 1));
		Shrinkable<Integer> shrinkable = ints.generate_OLD(source);

		shrinkable.shrink().forEach(s -> {
			assertThat(s.recording()).isLessThan(shrinkable.recording());
			// System.out.println("shrink recording: " + s.recording());
			// System.out.println("shrink value: " + s.value());
		});
	}

	@Example
	void shrinkListOfInts() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 5);

		// [-10, 50, -5]
		ListRecording listRecording = new ListRecording(
			List.of(
				new AtomRecording(10, 1),
				new AtomRecording(50, 0),
				new AtomRecording(5, 1)
			)
		);
		TreeRecording treeRecording = new TreeRecording(
			new AtomRecording(3), listRecording
		);
		GenSource source = new RecordedSource(treeRecording);

		Shrinkable<List<Integer>> shrinkable = listOfInts.generate_OLD(source);

		System.out.println("value: " + shrinkable.value());
		shrinkable.shrink().forEach(s -> {
			assertThat(s.recording()).isLessThan(shrinkable.recording());
			System.out.println("shrink recording: " + s.recording());
			System.out.println("shrink value    : " + s.value());
		});
	}

	@Example
	void shrinkIntWithProperty() {
		IntegerGenerator ints = new IntegerGenerator(-100000, 100000);

		// 9999
		GenSource source = new RecordedSource(new AtomRecording(9999, 0));
		Shrinkable<Integer> shrinkable = ints.generate_OLD(source);

		Function<List<Object>, PropertyExecutionResult> property = args -> {
			int i = (int) args.get(0);
			return i > 1000 ? PropertyExecutionResult.FAILED : PropertyExecutionResult.SUCCESSFUL;
		};

		assertThat(property.apply(List.of(shrinkable.value()))).isEqualTo(PropertyExecutionResult.FAILED);

		Shrinker shrinker = new Shrinker(List.of(shrinkable.asGeneric()), property);

		Shrinkable<Object> best = shrinkable.asGeneric();
		while (true) {
			Optional<List<Shrinkable<Object>>> next = shrinker.nextStep();
			if (next.isEmpty()) {
				break;
			}
			assertThat(next.get().get(0).compareTo(best)).isLessThan(0);
			best = next.get().get(0);
			//System.out.println("shrink: " + next.get());
		}
		assertThat(best.value()).isEqualTo(1001);

	}

	@Example
	void shrinkListWithProperty() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 5);

		// [-10, 50, -5]
		ListRecording listRecording = new ListRecording(
			List.of(
				new AtomRecording(10, 1),
				new AtomRecording(50, 0),
				new AtomRecording(5, 1)
			)
		);
		TreeRecording treeRecording = new TreeRecording(
			new AtomRecording(3), listRecording
		);
		GenSource source = new RecordedSource(treeRecording);

		Shrinkable<List<Integer>> shrinkable = listOfInts.generate_OLD(source);

		Function<List<Object>, PropertyExecutionResult> property = args -> {
			List<Integer> list = (List<Integer>) args.get(0);
			int sum = list.stream().mapToInt(i -> i).sum();
			return list.size() > 1 && sum != 0 ? PropertyExecutionResult.FAILED : PropertyExecutionResult.SUCCESSFUL;
		};

		assertThat(property.apply(List.of(shrinkable.value()))).isEqualTo(PropertyExecutionResult.FAILED);

		Shrinker shrinker = new Shrinker(List.of(shrinkable.asGeneric()), property);

		Shrinkable<Object> best = shrinkable.asGeneric();
		while (true) {
			Optional<List<Shrinkable<Object>>> next = shrinker.nextStep();
			if (next.isEmpty()) {
				break;
			}
			assertThat(next.get().get(0).compareTo(best)).isLessThan(0);
			best = next.get().get(0);
			System.out.println("shrink: " + next.get());
		}
		assertThat(best.value()).isEqualTo(List.of(0, 1));

	}

}