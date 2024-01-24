package jqwik2.internal.exhaustive;

import java.util.*;
import java.util.function.*;

import jqwik2.api.*;
import jqwik2.api.recording.*;

public class ExhaustiveFlatMap extends AbstractExhaustiveSource<GenSource.Tuple> {
	private final Function<GenSource, Optional<ExhaustiveSource<?>>> mappingFunction;
	private final ExhaustiveSource<?> sourceToMap;
	private Optional<? extends ExhaustiveSource<?>> optionalChild = Optional.empty();

	public ExhaustiveFlatMap(ExhaustiveSource<?> sourceToMap, Function<GenSource, Optional<ExhaustiveSource<?>>> mappingFunction) {
		this.mappingFunction = mappingFunction;
		this.sourceToMap = sourceToMap;
		creatAndChainChild();
	}

	private void creatAndChainChild() {
		optionalChild = mappingFunction.apply(sourceToMap.current());
		optionalChild.ifPresent(child -> {
			sourceToMap.chain(child);
			succ().ifPresent(child::setSucc);
		});
	}

	@Override
	public long maxCount() {
		long sum = 0;
		for (GenSource genSource : sourceToMap) {
			Optional<? extends ExhaustiveSource<?>> optionalChild = mappingFunction.apply(genSource);
			if (optionalChild.isPresent()) {
				sum += optionalChild.get().maxCount();
			} else {
				return Exhaustive.INFINITE;
			}
		}
		return sum;
	}

	@Override
	protected boolean tryAdvance() {
		if (!sourceToMap.advanceThisOrUp()) {
			return false;
		}
		creatAndChainChild();
		return true;
	}

	@Override
	public boolean advance() {
		Recording before = sourceToMap.recording();
		if (sourceToMap.advance()) {
			Recording after = sourceToMap.recording();
			if (!before.equals(after)) {
				creatAndChainChild();
			}
			return true;
		}
		if (prev().isEmpty()) {
			return false;
		}
		reset();
		return prev().get().advanceThisOrUp();
	}

	@Override
	public void reset() {
		sourceToMap.reset();
		creatAndChainChild();
	}

	@Override
	public ExhaustiveFlatMap clone() {
		return new ExhaustiveFlatMap(sourceToMap.clone(), mappingFunction);
	}

	@Override
	public void setSucc(Exhaustive<?> exhaustive) {
		super.setSucc(exhaustive);
		optionalChild.ifPresent(child -> child.setSucc(exhaustive));
	}

	@Override
	public Recording recording() {
		return Recording.tuple(
			sourceToMap.recording(),
			// TODO: Handle empty optional child, maybe child shouldn't be optional in the first place
			optionalChild.get().recording()
		);
	}
}
