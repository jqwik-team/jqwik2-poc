package jqwik2gen;

import java.util.*;

public sealed interface GenSource permits RandomGenSource, RecordedSource {
	int next(int max);

	GenSource child();
}

