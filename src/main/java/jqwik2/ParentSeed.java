package jqwik2;

import java.util.*;

public record ParentSeed(BaseSeed baseSeed, List<Seed> children) implements Seed {
	@Override
	public GenerationSource get() {
		return null;
	}
}
