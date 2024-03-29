package jqwik2.api.functions;

import java.util.*;

public interface Condition {
	boolean check(List<Object> params) throws Throwable;
}
