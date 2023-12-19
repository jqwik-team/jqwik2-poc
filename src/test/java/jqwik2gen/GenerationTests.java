package jqwik2gen;

import java.util.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class GenerationTests {

	@Example
	void smallInts() {
		Generator<Integer> gen0to10 = new IntegerGenerator(-10, 100);

		RandomGenSource source = new RandomGenSource(42);

		for (int i = 0; i < 10; i++) {
			Shrinkable<Integer> shrinkable = gen0to10.generate(source);
			assertThat(shrinkable).isNotNull();
			System.out.println("value=" + shrinkable.value());

			Integer regenerated = shrinkable.regenerate();
			System.out.println("regenerated=" + regenerated);

			assertThat(regenerated).isEqualTo(shrinkable.value());
		}
	}

	@Example
	void smallIntsWithRecorder() {
		Generator<Integer> gen0to10 = new IntegerGenerator(-10, 100);

		RandomGenSource source = new RandomGenSource(42);
		GenRecorder recorder = new GenRecorder(source);

		for (int i = 0; i < 10; i++) {
			Shrinkable<Integer> shrinkable = gen0to10.generate(recorder);
			assertThat(shrinkable).isNotNull();
			System.out.println("value=" + shrinkable.value());
			System.out.println("recorded=" + recorder.recording());
		}
	}

	@Example
	void intEdgeCases() {
		Generator<Integer> allInts = new IntegerGenerator(Integer.MIN_VALUE, Integer.MAX_VALUE);

		GenSource maxValueSource = new RecordedSource(new AtomRecording(Integer.MAX_VALUE, 0));
		GenSource minValueSource = new RecordedSource(new AtomRecording(Integer.MAX_VALUE, 2));

		assertThat(allInts.generate(maxValueSource).value())
			.isEqualTo(Integer.MAX_VALUE);
		assertThat(allInts.generate(minValueSource).value())
			.isEqualTo(Integer.MIN_VALUE);
	}

@Example
void listOfInts() {
	IntegerGenerator ints = new IntegerGenerator(-10, 100);
	Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 5);

	RandomGenSource source = new RandomGenSource(42);

	for (int i = 0; i < 10; i++) {
		Shrinkable<List<Integer>> shrinkable = listOfInts.generate(source);
		System.out.println("value=" + shrinkable.value());
		System.out.println("recording=" + shrinkable.recording());

		List<Integer> regenerated = shrinkable.regenerate();
		System.out.println("regenerated=" + regenerated);

		assertThat(regenerated).isEqualTo(shrinkable.value());
	}
}
}
