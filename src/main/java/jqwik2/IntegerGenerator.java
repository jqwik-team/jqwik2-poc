package jqwik2;

public class IntegerGenerator implements ValueGenerator<Integer> {

	@Override
	public Integer value(GenerationSource source) {
		return source.next(1)[0];
	}

}
