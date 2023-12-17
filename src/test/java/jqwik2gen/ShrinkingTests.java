package jqwik2gen;

import java.util.*;
import java.util.function.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

public class ShrinkingTests {

	@Example
	void generateWithUnsatisfyingGenSource() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);

		GenSource source = new RecordedSource(new AtomicRecording(10, 0));
		Shrinkable<Integer> shrinkable = ints.generate(source);
		assertThat(shrinkable.value()).isEqualTo(10);

		assertThatThrownBy(() -> {
			RecordedSource recorded = new RecordedSource(new AtomicRecording(100, 1));
			ints.generate(recorded);
		}).isInstanceOf(CannotGenerateException.class);

		assertThatThrownBy(() -> {
			RecordedSource recorded = new RecordedSource(new AtomicRecording(100));
			ints.generate(recorded);
		}).isInstanceOf(CannotGenerateException.class);
	}

	@Example
	void shrinkIntegers() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);

		// -10
		GenSource source = new RecordedSource(new AtomicRecording(10, 1));
		Shrinkable<Integer> shrinkable = ints.generate(source);

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
		TreeRecording treeRecording = new TreeRecording(
			new AtomicRecording(3),
			List.of(
				new AtomicRecording(10, 1),
				new AtomicRecording(50, 0),
				new AtomicRecording(5, 1)
			)
		);
		GenSource source = new RecordedSource(treeRecording);

		Shrinkable<List<Integer>> shrinkable = listOfInts.generate(source);

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
		GenSource source = new RecordedSource(new AtomicRecording(9999, 0));
		Shrinkable<Integer> shrinkable = ints.generate(source);

		Function<List<Object>, PropertyExecutionResult> property = args -> {
			int i = (int) args.get(0);
			return i > 1000 ? PropertyExecutionResult.FAILED : PropertyExecutionResult.SUCCESSFUL;
		};

		assertThat(property.apply(List.of(shrinkable.value()))).isEqualTo(PropertyExecutionResult.FAILED);

		Shrinker shrinker = new Shrinker(List.of(shrinkable), property);

		while (true) {
			Optional<List<Shrinkable<?>>> next = shrinker.nextStep();
			if (next.isEmpty()) {
				break;
			}
			System.out.println("shrink: " + next.get());
		}

	}

}