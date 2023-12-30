package jqwik2.api;

import java.util.*;

/**
 * A multi gen source is used to provide gen sources for more than one parameter.
 */
public interface MultiGenSource {

	List<GenSource> sources(int size);
}
