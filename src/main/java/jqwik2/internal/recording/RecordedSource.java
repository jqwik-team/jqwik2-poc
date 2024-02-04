package jqwik2.internal.recording;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;

public abstract sealed class RecordedSource<T extends Recording> implements GenSource {
	protected final GenSource backUpSource;
	protected final T recording;

	public static GenSource of(Recording recording) {
		return RecordedSource.of(recording, null);
	}

	public static GenSource of(Recording recording, GenSource backUpSource) {
		return switch (recording) {
			case ListRecording listRecording -> new RecordedList(listRecording, backUpSource);
			case TupleRecording tupleRecording -> new RecordedTuple(tupleRecording, backUpSource);
			case ChoiceRecording choiceRecording -> new RecordedChoice(choiceRecording, backUpSource);
			case null -> throw new IllegalArgumentException("Recording must not be null");
		};
	}

	private RecordedSource(T recording, GenSource backUpSource) {
		this.backUpSource = backUpSource;
		this.recording = recording;
	}

	@Override
	public Choice choice() {
		throw new CannotGenerateException("Source is not a choice");
	}

	@Override
	public Tuple tuple() {
		throw new CannotGenerateException("Source is not a tuple");
	}

	@Override
	public List list() {
		throw new CannotGenerateException("Source is not a list");
	}

	@Override
	public String toString() {
		return "RecordedSource{%s}".formatted(recording.toString());
	}

	private static final class RecordedList extends RecordedSource<ListRecording> implements GenSource.List {

		private final Iterator<Recording> elements;

		private RecordedList(ListRecording recording, GenSource backUpSource) {
			super(recording, backUpSource);
			this.elements = recording.elements().iterator();
		}

		@Override
		public List list() {
			return this;
		}

		@Override
		public GenSource nextElement() {
			if (elements.hasNext()) {
				return RecordedSource.of(elements.next(), backUpSource);
			}
			if (backUpSource != null) {
				return backUpSource.list().nextElement();
			}
			throw new CannotGenerateException("No more elements");
		}

	}

	private static final class RecordedTuple extends RecordedSource<TupleRecording> implements GenSource.Tuple {

		private final java.util.List<Recording> elements;
		private final Iterator<Recording> values;

		private RecordedTuple(TupleRecording recording, GenSource backUpSource) {
			super(recording, backUpSource);
			this.elements = recording.elements();
			this.values = elements.iterator();
		}

		@Override
		public Tuple tuple() {
			return this;
		}

		@Override
		public GenSource nextValue() {
			if (values.hasNext()) {
				return RecordedSource.of(values.next(), backUpSource);
			}
			if (backUpSource != null) {
				return backUpSource.list().nextElement();
			}
			throw new CannotGenerateException("No more values");
		}
	}

	private static final class RecordedChoice extends RecordedSource<ChoiceRecording> implements Choice {

		private final Iterator<Integer> noneOrOneChoice;

		private RecordedChoice(ChoiceRecording recording, GenSource backUpSource) {
			super(recording, backUpSource);
			noneOrOneChoice = recording.optionalChoice().stream().iterator();
		}

		@Override
		public Choice choice() {
			return this;
		}

		@Override
		public int choose(int maxExcluded) {
			if (noneOrOneChoice.hasNext()) {
				if (maxExcluded == 0) {
					return 0;
				}
				return noneOrOneChoice.next() % maxExcluded;
			} else {
				if (backUpSource != null) {
					return backUpSource.choice().choose(maxExcluded);
				}
				throw new CannotGenerateException("No more choices!");
			}
		}

		@Override
		public int choose(int maxExcluded, RandomChoice.Distribution ignore) {
			return choose(maxExcluded);
		}
	}
}