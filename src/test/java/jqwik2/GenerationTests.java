package jqwik2;

import java.util.*;

import jqwik2.api.*;

import net.jqwik.api.*;

import static jqwik2.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

class GenerationTests {

	@Example
	void smallInts() {
		Generator<Integer> minus10to100 = new IntegerGenerator(-10, 100);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			Integer value = minus10to100.generate(source);
			assertThat(value).isBetween(-10, 100);
			// System.out.println("value=" + value);
		}
	}

	@Example
	void positiveInts() {
		Generator<Integer> gen5to10 = new IntegerGenerator(5, 10);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			Integer value = gen5to10.generate(source);
			assertThat(value).isBetween(5, 10);
			// System.out.println("value=" + value);
		}
	}

	@Example
	void negativeInts() {
		Generator<Integer> minus20to0 = new IntegerGenerator(-20, 0);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 20; i++) {
			Integer value = minus20to0.generate(source);
			assertThat(value).isBetween(-20, 0);
			// System.out.println("value=" + value);
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
			// System.out.println("value=    " + value);
			// System.out.println("recorded= " + recorder.recording());
		}
	}

	@Example
	void positiveIntsWithRecorder() {
		Generator<Integer> gen5to100 = new IntegerGenerator(5, 10);
		RandomGenSource source = new RandomGenSource("42");

		for (int i = 0; i < 10; i++) {
			GenRecorder recorder = new GenRecorder(source);
			Integer value = gen5to100.generate(recorder);
			assertThat(value).isNotNull();
			// System.out.println("value=    " + value);
			// System.out.println("recorded= " + recorder.recording());
		}
	}

	@Example
	void generateFromRecording() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);

		GenSource source = new RecordedSource(atom(10, 0));
		Integer value = ints.generate(source);
		assertThat(value).isEqualTo(10);
	}

	@Example
	void generateWithUnsatisfyingGenSource() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);

		assertThatThrownBy(() -> {
			RecordedSource recorded = new RecordedSource(atom(100, 1));
			ints.generate(recorded);
		}).isInstanceOf(CannotGenerateException.class);

		assertThatThrownBy(() -> {
			RecordedSource recorded = new RecordedSource(atom(100));
			ints.generate(recorded);
		}).isInstanceOf(CannotGenerateException.class);
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
			List<Integer> value = listOfInts.generate(recorder);
			assertThat(value).isNotNull();
			System.out.println("value=    " + value);
			System.out.println("recorded= " + recorder.recording());
		}
	}
}
