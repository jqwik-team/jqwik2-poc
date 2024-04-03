package jqwik2.tdd;

import java.time.*;

import jqwik2.api.*;

class DrivingStrategyBuilder implements TddDrivingStrategy.Builder, Cloneable {

	private int maxTries = JqwikDefaults.defaultMaxTries();
	private Duration maxRuntime = JqwikDefaults.defaultMaxDuration();

	@Override
	protected DrivingStrategyBuilder clone() {
		var strategyBuilder = new DrivingStrategyBuilder();
		strategyBuilder.maxTries = maxTries;
		strategyBuilder.maxRuntime = maxRuntime;
		return strategyBuilder;
	}

	@Override
	public TddDrivingStrategy build() {
		return new DefaultDrivingStrategy(maxTries, maxRuntime);
	}

	@Override
	public TddDrivingStrategy.Builder withMaxTries(int maxTries) {
		DrivingStrategyBuilder clone = clone();
		clone.maxTries = maxTries;
		return clone;
	}

	@Override
	public TddDrivingStrategy.Builder withMaxRuntime(Duration maxRuntime) {
		DrivingStrategyBuilder clone = clone();
		clone.maxRuntime = maxRuntime;
		return clone;
	}
}
