package jqwik2gen;

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
}