package jqwik2.tdd;

import java.time.*;

public interface TddDrivingStrategy {

	int maxTries();

	Duration maxRuntime();

	interface Builder {
		TddDrivingStrategy build();

		TddDrivingStrategy.Builder withMaxTries(int maxTries);

		TddDrivingStrategy.Builder withMaxRuntime(Duration maxRuntime);
	}

	TddDrivingStrategy DEFAULT = builder().build();

	static TddDrivingStrategy.Builder builder() {
		return new DrivingStrategyBuilder();
	}

}
