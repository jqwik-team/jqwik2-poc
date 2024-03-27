package jqwik2.tdd;

public interface TDD {

	static TddProperty.Builder id(String id) {
		return new TddPropertyBuilder(id);
	}

}
