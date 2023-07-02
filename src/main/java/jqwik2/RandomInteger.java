package jqwik2;

public class RandomInteger implements RandomValue<Integer> {

	@Override
	public Integer value(RandomSource source) {
		return source.next(1)[0];
	}

}
