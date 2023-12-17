package jqwik2gen;

public sealed interface GenSource permits RandomGenSource, RecordedSource {
	int next(int max);

	GenSource child();
}

