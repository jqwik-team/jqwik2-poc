package jqwik2.internal.exhaustive;

import java.util.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveTuple extends AbstractExhaustiveSource<GenSource.Tuple> {

	private final List<? extends ExhaustiveSource<?>> values;

	public ExhaustiveTuple(List<? extends ExhaustiveSource<?>> values) {
		this.values = values;
		chainSources();
	}

	private void chainSources() {
		for (int i = 0; i < values.size(); i++) {
			ExhaustiveSource<?> current = values.get(i);
			if (i > 0) {
				values.get(i - 1).chain(current);
			}
		}
	}

	@Override
	public long maxCount() {
		return values.getFirst().maxCount();
	}

	@Override
	protected boolean tryAdvance() {
		return values.getLast().advanceThisOrUp();
	}

	@Override
	public void reset() {
		values.forEach(ExhaustiveSource::reset);
	}

	@Override
	public ExhaustiveSource<GenSource.Tuple> clone() {
		List<? extends ExhaustiveSource<?>> clonedValues = values.stream().map(ExhaustiveSource::clone).toList();
		return new ExhaustiveTuple(clonedValues);
	}

	@Override
	public boolean advance() {
		if (values.getFirst().advance()) {
			return true;
		}
		if (prev().isEmpty()) {
			return false;
		}
		return prev().get().advanceThisOrUp();
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		values.getLast().setSucc(exhaustive);
		super.setSucc(exhaustive);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("ExhaustiveTuple{");
		sb.append("values=").append(values);
		sb.append('}');
		return sb.toString();
	}

	@Override
	public Recording recording() {
		return Recording.tuple(
			values.stream().map(ExhaustiveSource::recording).toList()
		);
	}
}
