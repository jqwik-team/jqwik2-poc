package jqwik2.internal.growing;

import java.util.*;
import java.util.stream.*;

class GrowingGenSource extends AbstractGrowingSource<GrowingGenSource> {

	private GrowingSourceContainer currentSource;

	GrowingGenSource() {
		this(new GrowingSourceContainer());
	}

	public GrowingGenSource(GrowingSourceContainer growingSourceContainer) {
		this.currentSource = growingSourceContainer;
	}

	@Override
	public Choice choice() {
		return currentSource.get(Choice.class, GrowingChoice::new);
	}

	@Override
	public Tuple tuple() {
		return currentSource.get(Tuple.class, GrowingTuple::new);
	}

	@Override
	public List list() {
		return currentSource.get(List.class, GrowingList::new);
	}

	@Override
	public Set<GrowingGenSource> grow() {
		return currentSource.grow().stream().map(GrowingGenSource::new).collect(Collectors.toSet());
	}

	@Override
	public GrowingGenSource copy() {
		return new GrowingGenSource(currentSource.copy());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GrowingGenSource source = (GrowingGenSource) o;
		return Objects.equals(currentSource, source.currentSource);
	}

	@Override
	public int hashCode() {
		return currentSource != null ? currentSource.hashCode() : 0;
	}
}
