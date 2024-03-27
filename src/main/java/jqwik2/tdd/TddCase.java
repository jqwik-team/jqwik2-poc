package jqwik2.tdd;

import java.util.*;
import java.util.function.*;

import jqwik2.api.description.*;

record TddCase(String label, Condition condition, Consumer<List<Object>> verifier) {
}
