package jqwik2gen;

public sealed interface GenSource permits RandomGenSource, RecordedSource {

	// Todo: Distribute those methods to sub-interfaces: AtomSource, ListSource, TreeSource

	int next(int max);
	GenSource child();
	GenSource next();

	// Todo: Implement factory methods: atom(), list(), tree()
}

