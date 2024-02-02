package jqwik2.internal.growing;

import java.util.function.*;

import jqwik2.api.*;
import jqwik2.internal.*;

class GrowingSourceContainer implements GrowingSource {

	private GrowingSource currentSource;
	private boolean resourceRequested = false;

	@SuppressWarnings("unchecked")
	<T extends GenSource> T get(Class<T> genSourceType, Supplier<GrowingSource> genSourceSupplier) {
		if (resourceRequested) {
			throw new CannotGenerateException("Already requested a resource");
		}
		if (currentSource == null) {
			currentSource = genSourceSupplier.get();
		} else if (!genSourceType.isInstance(currentSource)) {
			throw new CannotGenerateException("Source is not an atom");
		}
		resourceRequested = true;
		return (T) currentSource;
	}

	@Override
	public boolean advance() {
		resourceRequested = false;
		if (currentSource == null) {
			return false;
		}
		return currentSource.advance();
	}

	@Override
	public void reset() {
		resourceRequested = false;
		if (currentSource == null) {
			return;
		}
		currentSource.reset();
	}
}
