package jqwik2.internal.description;

import java.util.*;

import jqwik2.api.description.*;

record PropertyClassifier(List<Case<?>> cases) implements Classifier {
}
