package jqwik2gen;

import java.util.*;

import org.assertj.core.api.*;

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
			// System.out.println("shrink: " + s.value());
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

		shrinkable.shrink().forEach(s -> {
			//assertThat(s.recording()).isLessThan(shrinkable.recording());
			System.out.println("shrink: " + s.value());
		});
	}

}