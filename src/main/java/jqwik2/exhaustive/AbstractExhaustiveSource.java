package jqwik2.exhaustive;

import jqwik2.api.*;

abstract class AbstractExhaustiveSource<T extends GenSource> extends AbstractExhaustive<ExhaustiveSource<T>> implements ExhaustiveSource<T> {

}
