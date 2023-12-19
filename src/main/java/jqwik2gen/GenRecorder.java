package jqwik2gen;

import java.util.*;
import java.util.stream.*;

public class GenRecorder extends AbstractRecorder<GenSource> {

	private AbstractRecorder<? extends GenSource> concreteRecorder;

	public GenRecorder(GenSource source) {
		super(source);
	}

	public SourceRecording recording() {
		if (concreteRecorder == null) {
			throw new IllegalStateException("Recording has not been started");
		}
		return concreteRecorder.recording();
	}

	@Override
	public Atom atom() {
		concreteRecorder = record(Atom.class, source.atom());
		return (Atom) concreteRecorder;
	}

	@Override
	public List list() {
		concreteRecorder = record(List.class, source.list());
		return (List) concreteRecorder;
	}

	@Override
	public Tree tree() {
		concreteRecorder = record(Tree.class, source.tree());
		return (Tree) concreteRecorder;
	}

	static class AtomRecorder extends AbstractRecorder<Atom> implements Atom {

		private final java.util.List<Integer> seeds = new ArrayList<>();

		AtomRecorder(Atom source) {
			super(source);
		}

		@Override
		public Atom atom() {
			return this;
		}

		@Override
		SourceRecording recording() {
			return new AtomRecording(seeds);
		}

		@Override
		public int choice(int max) {
			int choice = source.choice(max);
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
		SourceRecording recording() {
			java.util.List<SourceRecording> elementRecordings = elements.stream()
																		.map(AbstractRecorder::recording)
																		.collect(Collectors.toList());
			return new ListRecording(elementRecordings);
		}

		@Override
		public <T extends GenSource> T nextElement(Class<T> sourceType) {
			AbstractRecorder<?> next = record(sourceType, source.nextElement());
			elements.add(next);
			return (T) next;
		}

	}

	static class TreeRecorder extends AbstractRecorder<Tree> implements Tree {

		private AbstractRecorder<?> head;
		private AbstractRecorder<?> child;

		TreeRecorder(Tree source) {
			super(source);
		}

		@Override
		public Tree tree() {
			return this;
		}

		@Override
		SourceRecording recording() {
			if (head == null || child == null) {
				throw new IllegalStateException("Recording has not been finished");
			}
			return new TreeRecording(head.recording(), child.recording());
		}

		@Override
		public <T extends GenSource> T head(Class<T> sourceType) {
			head = record(sourceType, source.head(sourceType));
			return (T) head;
		}

		@Override
		public <T extends GenSource> T child(Class<T> sourceType) {
			child = record(sourceType, source.head(sourceType));
			return (T) child;
		}
	}

}

abstract class AbstractRecorder<T extends GenSource> implements GenSource {

	final T source;

	AbstractRecorder(T source) {
		this.source = source;
	}

	abstract SourceRecording recording();

	@Override
	public Atom atom() {
		throw new UnsupportedOperationException("Should never be called");
	}

	@Override
	public List list() {
		throw new UnsupportedOperationException("Should never be called");
	}

	@Override
	public Tree tree() {
		throw new UnsupportedOperationException("Should never be called");
	}

	AbstractRecorder<?> record(Class<? extends GenSource> sourceType, GenSource source) {
		if (sourceType.equals(Atom.class)) {
			return new GenRecorder.AtomRecorder((Atom) source);
		} else if (sourceType.equals(List.class)) {
			return new GenRecorder.ListRecorder((List) source);
		} else if (sourceType.equals(Tree.class)) {
			return new GenRecorder.TreeRecorder((Tree) source);
		} else if (sourceType.equals(GenSource.class)) {
			return new GenRecorder(source);
		}
		throw new IllegalArgumentException("Invalid source type: " + source.getClass());
	}

}

