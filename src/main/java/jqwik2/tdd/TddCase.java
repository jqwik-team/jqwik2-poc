package jqwik2.tdd;

import jqwik2.api.description.*;

record TddCase(String label, PropertyDescription property, jqwik2.api.functions.Condition condition) {
}
