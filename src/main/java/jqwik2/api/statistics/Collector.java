package jqwik2.api.statistics;

import java.util.function.*;

public interface Collector {

	/**
	 * Call this method to record an entry for statistical data about generated values.
	 *
	 * @param values Can be anything. The list of these values is considered
	 *               a key for the reported table of frequencies. Constraints:
	 *               <ul>
	 *               <li>There must be at least one value</li>
	 *               <li>The number of values for the same collector (i.e. same label)
	 *               must always be the same in a single property</li>
	 *               <li>Values can be {@code null}</li>
	 *               </ul>
	 * @return The current instance of collector to allow a fluent coverage API
	 * @throws IllegalArgumentException if one of the constraints on {@code values} is violated
	 */
	Collector collect(Object... values);

	/**
	 * Perform coverage checking for successful property on statistics.
	 *
	 * @param coverage Code that consumes a {@linkplain Coverage} object
	 */
	void coverage(Consumer<Coverage> coverage);
}
