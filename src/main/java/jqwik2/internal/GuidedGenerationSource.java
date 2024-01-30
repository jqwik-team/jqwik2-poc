package jqwik2.internal;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;

public class GuidedGenerationSource implements IterableSampleSource {
	private final Supplier<GuidedGeneration> guidanceSupplier;

	public GuidedGenerationSource(Supplier<GuidedGeneration> guidanceSupplier) {
		this.guidanceSupplier = guidanceSupplier;
	}

	@Override
	public boolean stopWhenFalsified() {
		return false;
	}

	@Override
	public Iterator<SampleSource> iterator() {
		return guidanceSupplier.get();
	}

}
