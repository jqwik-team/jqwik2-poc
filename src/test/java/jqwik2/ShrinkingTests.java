package jqwik2;

import java.util.*;
import java.util.function.*;

import jqwik2.api.Shrinkable;
import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;
import jqwik2.internal.generators.*;
import jqwik2.internal.recording.*;
import jqwik2.internal.shrinking.*;
import org.opentest4j.*;

import net.jqwik.api.*;

import static jqwik2.api.TryExecutionResult.Status.*;
import static jqwik2.api.recording.Recording.list;
import static jqwik2.api.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

public class ShrinkingTests {

	@Example
	void checkTryableFromFunction() {
		Tryable booleanSucceed = Tryable.from(args -> true);
		assertThat(booleanSucceed.apply(List.of())).isEqualTo(new TryExecutionResult(SATISFIED));

		Tryable booleanFailed = Tryable.from(args -> false);
		assertThat(booleanFailed.apply(List.of())).isEqualTo(new TryExecutionResult(FALSIFIED));

		Tryable booleanBoxedFailed = Tryable.from(args -> Boolean.FALSE);
		assertThat(booleanBoxedFailed.apply(List.of())).isEqualTo(new TryExecutionResult(FALSIFIED));

		AssertionFailedError failedError = new AssertionFailedError("failed");
		Tryable assertionFailed = Tryable.from((Consumer<List<Object>>) args -> {
			throw failedError;
		});
		assertThat(assertionFailed.apply(List.of()))
			.isEqualTo(new TryExecutionResult(FALSIFIED, failedError));

		Tryable invalid = Tryable.from((Consumer<List<Object>>) args -> {
			throw new TestAbortedException();
		});
		assertThat(invalid.apply(List.of()).status()).isEqualTo(INVALID);
	}

	@Example
	void shrinkIntegers() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);

		// -10
		GenSource source = RecordedSource.of(atom(10, 1));

		Shrinkable<Integer> shrinkable = new ShrinkableGenerator<>(ints).generate(source);

		shrinkable.shrink().forEach(s -> {
			assertThat(s.recording()).isLessThan(shrinkable.recording());
			// System.out.println("shrink recording: " + s.recording());
			// System.out.println("shrink value: " + s.value());
		});
	}

	@Example
	void shrinkListOfInts() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 0, 5);

		// [-10, 50, -5]
		ListRecording listRecording = list(
			atom(10, 1),
			atom(50, 0),
			atom(5, 1)
		);
		TupleRecording tupleRecording = tuple(
			atom(3), listRecording
		);
		GenSource source = RecordedSource.of(tupleRecording);

		Shrinkable<List<Integer>> shrinkable = new ShrinkableGenerator<>(listOfInts).generate(source);

		// System.out.println("value: " + shrinkable.value());
		shrinkable.shrink().forEach(s -> {
			assertThat(s.recording()).isLessThan(shrinkable.recording());
			// System.out.println("shrink recording: " + s.recording());
			// System.out.println("shrink value    : " + s.value());
		});
	}

	@Example
	void shrinkIntWithProperty() {
		IntegerGenerator ints = new IntegerGenerator(-100000, 100000);

		// 9999
		GenSource source = RecordedSource.of(atom(9999, 0));

		SampleGenerator sampleGenerator = SampleGenerator.from(ints);
		Sample sample = sampleGenerator.generate(List.of(source)).orElseThrow();

		Tryable tryable = Tryable.from(args -> {
			int i = (int) args.get(0);
			assertThat(i > 1000).isFalse();
		});
		TryExecutionResult tryResult = tryable.apply(sample);
		assertThat(tryResult.status()).isEqualTo(FALSIFIED);
		FalsifiedSample falsifiedSample = new FalsifiedSample(sample, tryResult.throwable());

		final FalsifiedSample[] previousBest = new FalsifiedSample[] {falsifiedSample};
		Consumer<FalsifiedSample> forEachShrinkStep = falsified -> {
			// System.out.println("falsified: " + falsified.sample().values());
			assertThat(falsified.compareTo(previousBest[0])).isLessThan(0);
		};

		FullShrinker shrinker = new FullShrinker(falsifiedSample, tryable);
		FalsifiedSample best = shrinker.shrinkToEnd(forEachShrinkStep);
		assertThat(best.values()).isEqualTo(List.of(1001));
	}

	@Example
	void shrinkListWithProperty() {
		IntegerGenerator ints = new IntegerGenerator(-10, 100);
		Generator<List<Integer>> listOfInts = new ListGenerator<>(ints, 0, 5);

		// [-10, 50, -5]
		ListRecording listRecording = list(
			atom(10, 1),
			atom(50, 0),
			atom(5, 1)
		);
		TupleRecording tupleRecording = tuple(
			atom(3), listRecording
		);
		GenSource source = RecordedSource.of(tupleRecording);

		SampleGenerator sampleGenerator = SampleGenerator.from(listOfInts);
		Sample sample = sampleGenerator.generate(List.of(source)).orElseThrow();

		Tryable tryable = Tryable.from(args -> {
			List<Integer> list = (List<Integer>) args.get(0);
			int sum = list.stream().mapToInt(i -> i).sum();
			assertThat(list.size() > 1 && sum != 0).isFalse();
		});
		TryExecutionResult tryResult = tryable.apply(sample);
		assertThat(tryResult.status()).isEqualTo(FALSIFIED);
		FalsifiedSample falsifiedSample = new FalsifiedSample(sample, tryResult.throwable());

		final FalsifiedSample[] previousBest = new FalsifiedSample[] {falsifiedSample};
		Consumer<FalsifiedSample> forEachShrinkStep = falsified -> {
			// System.out.println("falsified: " + falsified.sample().values());
			assertThat(falsified.compareTo(previousBest[0])).isLessThan(0);
		};

		FullShrinker shrinker = new FullShrinker(falsifiedSample, tryable);
		FalsifiedSample best = shrinker.shrinkToEnd(forEachShrinkStep);
		assertThat(best.values()).isEqualTo(List.of(List.of(0, 1)));

	}

	@Example
	void shrinkSeveralParametersWithProperty() {
		IntegerGenerator gen1 = new IntegerGenerator(-100000, 100000);
		IntegerGenerator gen2 = new IntegerGenerator(0, 100);

		GenSource source1 = RecordedSource.of(atom(9999, 1)); // -9999
		GenSource source2 = RecordedSource.of(atom(50)); // 50

		SampleGenerator sampleGenerator = SampleGenerator.from(gen1, gen2);
		Sample sample = sampleGenerator.generate(List.of(source1, source2)).orElseThrow();

		Tryable tryable = Tryable.from(args -> {
			int i1 = (int) args.get(0);
			int i2 = (int) args.get(1);
			assertThat(i1 < -100 && i2 > 10).isFalse();
		});
		TryExecutionResult tryResult = tryable.apply(sample);
		assertThat(tryResult.status()).isEqualTo(FALSIFIED);
		FalsifiedSample falsifiedSample = new FalsifiedSample(sample, tryResult.throwable());

		final FalsifiedSample[] previousBest = new FalsifiedSample[] {falsifiedSample};
		Consumer<FalsifiedSample> forEachShrinkStep = falsified -> {
			// System.out.println("falsified: " + falsified.sample().values());
			assertThat(falsified.compareTo(previousBest[0])).isLessThan(0);
		};

		FullShrinker shrinker = new FullShrinker(falsifiedSample, tryable);
		FalsifiedSample best = shrinker.shrinkToEnd(forEachShrinkStep);
		assertThat(best.values()).isEqualTo(List.of(-101, 11));
	}

	@Example
	void shrinkWithRejectedAssumptions() {
		IntegerGenerator ints = new IntegerGenerator(-100000, 100000);

		// 9999
		GenSource source = RecordedSource.of(atom(9999, 0));

		SampleGenerator sampleGenerator = SampleGenerator.from(ints);
		Sample sample = sampleGenerator.generate(List.of(source)).orElseThrow();

		Tryable tryable = Tryable.from(args -> {
			int i = (int) args.get(0);
			if (i % 3 != 0) throw new TestAbortedException();
			assertThat(i).isLessThan(1000);
		});
		TryExecutionResult tryResult = tryable.apply(sample);
		assertThat(tryResult.status()).isEqualTo(FALSIFIED);
		FalsifiedSample falsifiedSample = new FalsifiedSample(sample, tryResult.throwable());

		final FalsifiedSample[] previousBest = new FalsifiedSample[] {falsifiedSample};
		Consumer<FalsifiedSample> forEachShrinkStep = falsified -> {
			// System.out.println("falsified: " + falsified.sample().values());
			assertThat(falsified.compareTo(previousBest[0])).isLessThan(0);
		};

		FullShrinker shrinker = new FullShrinker(falsifiedSample, tryable);
		FalsifiedSample best = shrinker.shrinkToEnd(forEachShrinkStep);
		assertThat(best.values()).isEqualTo(List.of(1002));
	}

}