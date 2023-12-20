package jqwik2;

import java.util.*;

import net.jqwik.api.*;

import static org.assertj.core.api.Assertions.*;

class GenerationTests {

	@Example
	void smallInts() {
		Generator<Integer> gen0to10 = new IntegerGenerator(-10, 100);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			Integer value = gen0to10.generate(source);
			assertThat(value).isNotNull();
			System.out.println("value=" + value);
		}
	}

	@Example
	void smallIntsWithRecorder() {
		Generator<Integer> gen0to10 = new IntegerGenerator(-100, 100);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			GenRecorder recorder = new GenRecorder(source);
			Integer value = gen0to10.generate(recorder);
			assertThat(value).isNotNull();
			System.out.println("value=    " + value);
			System.out.println("recorded= " + recorder.recording());
		}
	}

	@Example
	void intEdgeCases() {
		Generator<Integer> allInts = new IntegerGenerator(Integer.MIN_VALUE, Integer.MAX_VALUE);

		GenSource maxValueSource = new RecordedSource(new AtomRecording(Integer.MAX_VALUE, 0));
		GenSource minValueSource = new RecordedSource(new AtomRecording(Integer.MAX_VALUE, 2));

		assertThat(allInts.generate(maxValueSource))
			.isEqualTo(Integer.MAX_VALUE);
		assertThat(allInts.generate(minValueSource))
			.isEqualTo(Integer.MIN_VALUE);
	}

	@Example
	void listOfInts() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 5);

		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			List<Integer> value = listOfInts.generate(source);
			System.out.println("value=" + value);
		}
	}

	@Example
	void listOfIntsWithRecorder() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 5);

		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			GenRecorder recorder = new GenRecorder(source);
			List<Integer> shrinkable = listOfInts.generate(recorder);
			assertThat(shrinkable).isNotNull();
			System.out.println("value=    " + shrinkable);
			System.out.println("recorded= " + recorder.recording());
		}
	}
}
