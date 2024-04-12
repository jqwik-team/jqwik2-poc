package jqwik2.api.statistics;

public record StatisticalError(double alpha, double beta) {

	@Override
	public String toString() {
		return "(alpha=%s, beta=%s)".formatted(alpha, beta);
	}
}
