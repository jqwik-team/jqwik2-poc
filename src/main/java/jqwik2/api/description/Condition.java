package jqwik2.api.description;

import java.util.*;

public interface Condition {
	boolean check(List<Object> params) throws Throwable;
}
