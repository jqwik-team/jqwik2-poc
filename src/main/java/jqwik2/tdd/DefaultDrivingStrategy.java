package jqwik2.tdd;

import java.time.*;

record DefaultDrivingStrategy(int maxTries, Duration maxRuntime) implements TddDrivingStrategy {
}
