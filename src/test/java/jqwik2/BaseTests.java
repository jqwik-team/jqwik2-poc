package jqwik2;

import net.jqwik.api.*;

class BaseTests {

	@Example
	void test_should_succeed() {}

	@Group
	class GroupedTests {
		@Example
		void nested_test() {}
	}
}
