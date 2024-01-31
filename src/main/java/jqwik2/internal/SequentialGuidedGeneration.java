package jqwik2.internal;

import java.util.*;

import jqwik2.api.*;

public abstract class SequentialGuidedGeneration implements GuidedGeneration {

	private volatile boolean started = false;
	private volatile boolean guidanceArrived = false;
	private volatile boolean proceedGeneration = true;

	/**
	 * Return the initial source for the first sample generation.
	 */
	protected abstract SampleSource initialSource();

	/**
	 * Handle the guidance for the previous sample generation.
	 * Return true if the next sample generation should be started.
	 */
	protected abstract boolean handleResult(TryExecutionResult result, Sample sample);

	/**
	 * Provide the source for the next sample generation.
	 */
	protected abstract SampleSource nextSource();

	@Override
	public final boolean hasNext() {
		synchronized (this) {
			if (!started) {
				return true;
			}
			while (!guidanceArrived) {
				try {
					this.wait();
				} catch (InterruptedException ignore) {
				}
			}
			return proceedGeneration;
		}
	}

	@Override
	public final SampleSource next() {
		synchronized (this) {
			if (!started) {
				started = true;
				return initialSource();
			}
			if (!proceedGeneration) {
				throw new NoSuchElementException();
			}
			guidanceArrived = false;
			return nextSource();
		}
	}

	@Override
	public final void guide(TryExecutionResult result, Sample sample) {
		synchronized (this) {
			guidanceArrived = true;
			proceedGeneration = handleResult(result, sample);
			this.notifyAll();
		}
	}

}
