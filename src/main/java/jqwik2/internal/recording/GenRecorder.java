package jqwik2.internal.recording;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;
import jqwik2.internal.*;

public class GenRecorder extends AbstractRecorder<GenSource> {

	private AbstractRecorder<? extends GenSource> concreteRecorder;

	public GenRecorder(GenSource source) {
		super(source);
	}

	public GenRecorder swapInnerSource(Function<GenSource, GenSource> swap) {
		if (concreteRecorder != null) {
			throw new IllegalStateException("Recording has already been started");
		}
		source = swap.apply(source);
		return this;
	}

	public Recording recording() {
		if (concreteRecorder == null) {
			return Recording.EMPTY;
		}
		return concreteRecorder.recording();
	}

	@Override
	public Atom atom() {
		concreteRecorder = new AtomRecorder(source.atom());
		return (Atom) concreteRecorder;
	}

	@Override
	public List list() {
		concreteRecorder = new ListRecorder(source.list());
		return (List) concreteRecorder;
	}

	@Override
	public Tuple tuple() {
		concreteRecorder = new TupleRecorder(source.tuple());
		return (Tuple) concreteRecorder;
	}

	static class AtomRecorder extends AbstractRecorder<Atom> implements Atom {

		private final java.util.List<Integer> seeds = new ArrayList<>(5);

		AtomRecorder(Atom source) {
			super(source);
		}

		@Override
		public Atom atom() {
			return this;
		}

		@Override
		Recording recording() {
			return new AtomRecording(seeds);
		}

		@Override
		public int choose(int maxExcluded) {
			int choice = source.choose(maxExcluded);
			seeds.add(choice);
			return choice;
		}

		@Override
		public int choose(int maxExcluded, RandomChoice.Distribution distribution) {
			int choice = source.choose(maxExcluded, distribution);
			seeds.add(choice);
			return choice;
		}

		@Override
		public int choose(int maxExcluded, java.util.List<Integer> weights) {
			int choice = source.choose(maxExcluded, weights);
			seeds.add(choice);
			return choice;
		}
	}

	static class ListRecorder extends AbstractRecorder<List> implements List {

		private final java.util.List<AbstractRecorder<?>> elements = new ArrayList<>();

		ListRecorder(List source) {
			super(source);
		}

		@Override
		public List list() {
			return this;
		}

		@Override
		Recording recording() {
			java.util.List<Recording> elementRecordings = elements.stream()
																  .map(AbstractRecorder::recording)
																  .collect(Collectors.toList());
			return Recording.list(elementRecordings);
		}

		@Override
		public GenSource nextElement() {
			AbstractRecorder<?> next = new GenRecorder(source.nextElement());
			elements.add(next);
			return next;
		}

	}

	static class TupleRecorder extends AbstractRecorder<Tuple> implements Tuple {

		private final java.util.List<AbstractRecorder<?>> values = new ArrayList<>();

		TupleRecorder(Tuple source) {
			super(source);
		}

		@Override
		public Tuple tuple() {
			return this;
		}

		@Override
		Recording recording() {
			java.util.List<Recording> elementRecordings = new ArrayList<>();
			for (AbstractRecorder<?> value : values) {
				Recording recording = value.recording();
				elementRecordings.add(recording);
			}
			return Recording.tuple(elementRecordings);
		}

		@Override
		public GenSource nextValue() {
			AbstractRecorder<?> next = new GenRecorder(source.nextValue());
			values.add(next);
			return next;
		}
	}

}

abstract class AbstractRecorder<T extends GenSource> implements GenSource {

	T source;

	AbstractRecorder(T source) {
		this.source = source;
	}

	abstract Recording recording();

	@Override
	public Atom atom() {
		throw new UnsupportedOperationException("Should never be called");
	}

	@Override
	public List list() {
		throw new UnsupportedOperationException("Should never be called");
	}

	@Override
	public Tuple tuple() {
		throw new UnsupportedOperationException("Should never be called");
	}

}

