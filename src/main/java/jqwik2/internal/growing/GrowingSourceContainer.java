package jqwik2.internal.growing;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingSourceContainer implements GrowingSource<GrowingSourceContainer> {

	private GrowingSource<?> source;
	private boolean resourceRequested = false;

	GrowingSourceContainer() {
		this(null);
	}

	GrowingSourceContainer(GrowingSource<?> source) {
		this.source = source;
	}

	@SuppressWarnings("unchecked")
	<T extends GenSource> T get(Class<T> genSourceType, Supplier<GrowingSource<?>> genSourceSupplier) {
		if (resourceRequested) {
			throw new CannotGenerateException("Already requested a resource");
		}
		if (!genSourceType.isInstance(source)) {
			source = genSourceSupplier.get();
		}
		resourceRequested = true;
		return (T) source;
	}

	@Override
	public Set<GrowingSourceContainer> grow() {
		if (source == null) {
			return Set.of();
		}
		return source.grow().stream()
				.map(GrowingSourceContainer::new)
				.collect(Collectors.toSet());
	}

	@Override
	public GrowingSourceContainer copy() {
		return new GrowingSourceContainer(source == null ? null : source.copy());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GrowingSourceContainer that = (GrowingSourceContainer) o;
		return Objects.equals(source, that.source);
	}

	@Override
	public int hashCode() {
		return source != null ? source.hashCode() : 0;
	}
}
