package jqwik2.internal.growing;

import java.util.function.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingSourceContainer implements GrowingSource {

	private GrowingSource source;
	private boolean resourceRequested = false;

	@SuppressWarnings("unchecked")
	<T extends GenSource> T get(Class<T> genSourceType, Supplier<GrowingSource> genSourceSupplier) {
		if (resourceRequested) {
			throw new CannotGenerateException("Already requested a resource");
		}
		if (source == null) {
			source = genSourceSupplier.get();
		} else if (!genSourceType.isInstance(source)) {
			throw new CannotGenerateException("Source is not an atom");
		}
		resourceRequested = true;
		return (T) source;
	}

	@Override
	public void next() {
		resourceRequested = false;
		if (source == null) {
			return;
		}
		source.next();
	}

	@Override
	public boolean advance() {
		if (source == null) {
			return false;
		}
		return source.advance();
	}

	@Override
	public void reset() {
		if (source == null) {
			return;
		}
		source.reset();
	}

}
