package jqwik2.internal.sprt;

import java.util.stream.*;

public abstract class SPRT {

	public static void main(String[] args) {
		var values = new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7};
		var test = new SPRTNormal(0.05, 0.2, 0, 1, values, 1);
		test.getResult(10, "start");
	}

	protected double alpha;
	protected double beta;
	protected double h0;
	protected double h1;
	protected double[] values;
	protected double[] cumValues;
	protected double variance;
	protected double upperCritical;
	protected double lowerCritical;
	protected int numObservation;
	protected int[] seqObservation;
	protected int[] x;
	protected double[] yl;
	protected double[] yu;
	protected String decision;
	protected double testStatistic;
	protected double[] lowerBoundary;
	protected double[] upperBoundary;

	protected abstract void calBoundary();

	protected abstract void __checkOtherArgs();

	public SPRT(double alpha, double beta, double h0, double h1, double[] values, double variance) {
		this.alpha = alpha;
		this.beta = beta;
		this.h0 = h0;
		this.h1 = h1;
		this.values = values;
		this.cumValues = cumsum(values);
		this.variance = variance;
		this.upperCritical = Math.log((1 - beta) / alpha);
		this.lowerCritical = Math.log(beta / (1 - alpha));
		this.numObservation = values.length;
		this.seqObservation = IntStream.range(1, numObservation + 1).toArray();
		this.x = IntStream.range(0, numObservation + 2).toArray();
		this.yl = new double[numObservation + 2];
		this.yu = new double[numObservation + 2];
		this.decision = null;
		this.checkCommonArgs();
		this.__checkOtherArgs();
		this.calBoundary();
		this.seqTest();
	}

	private double[] cumsum(double[] values) {
		var cumsum = new double[values.length];
		var sum = 0;
		for (int i = 0; i < values.length; i++) {
			sum += values[i];
			cumsum[i] = sum;
		}
		return cumsum;
	}

	private void checkCommonArgs() {
		if (!(0 < alpha && alpha < 1) || !(0 < beta && beta < 1)) {
			System.err.println("Type I error rate and type II error rate are between 0 and 1!");
			System.exit(1);
		}
	}

	public void getResult(int nobs, String start) {
		System.out.println("Decision:\t" + decision + "\n");
		DataFrame roundedDataFrame = new DataFrame(
			new double[][]{cumValues, lowerBoundary, upperBoundary},
			new String[]{"values", "lower", "upper"},
			seqObservation
		);
		roundedDataFrame.setIndexName("n");
		roundedDataFrame = roundedDataFrame.round(3);
		System.out.println(roundedDataFrame.tail(nobs));
	}

	private void seqTest() {
		testStatistic = cumValues[numObservation - 1];
		if (testStatistic > upperBoundary[numObservation - 1]) {
			decision = "Reject";
		} else if (testStatistic < lowerBoundary[numObservation - 1]) {
			decision = "Accept";
		} else {
			decision = "Continue";
		}
		int header = Math.min(10, numObservation);
		getResult(header, "end");
	}
}

class SPRTNormal extends SPRT {

	private double slope;
	private double lowerIntercept;
	private double upperIntercept;

	public SPRTNormal(double alpha, double beta, double h0, double h1, double[] values, double variance) {
		super(alpha, beta, h0, h1, values, variance);
	}

	@Override
	protected void calBoundary() {
		slope = (h1 + h0) / 2;
		lowerIntercept = lowerCritical * variance / (h1 - h0);
		upperIntercept = upperCritical * variance / (h1 - h0);
		lowerBoundary = IntStream.of(seqObservation).mapToDouble(i -> i * slope + lowerIntercept).toArray();
		upperBoundary = IntStream.of(seqObservation).mapToDouble(i -> i * slope + upperIntercept).toArray();
		yl = IntStream.of(x).mapToDouble(i -> i * slope + lowerIntercept).toArray();
		yu = IntStream.of(x).mapToDouble(i -> i * slope + upperIntercept).toArray();
	}

	@Override
	protected void __checkOtherArgs() {
		if (variance <= 0) {
			System.err.println("Variance of normal distribution is positive!");
			System.exit(1);
		}
	}
}
