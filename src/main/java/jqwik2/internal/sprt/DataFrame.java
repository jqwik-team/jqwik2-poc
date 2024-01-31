package jqwik2.internal.sprt;

import java.text.*;
import java.util.*;

public class DataFrame {

	private double[][] data;
	private String[] columns;
	private int[] index;
	private String indexName;

	public DataFrame(double[][] data, String[] columns, int[] index) {
		if (data.length != columns.length || data.length > 0 && data[0].length != index.length) {
			throw new IllegalArgumentException("Invalid dimensions for DataFrame");
		}

		this.data = Arrays.copyOf(data, data.length);
		this.columns = Arrays.copyOf(columns, columns.length);
		this.index = Arrays.copyOf(index, index.length);
	}

	public void setIndexName(String indexName) {
		this.indexName = indexName;
	}

	public void setColumns(String[] columns) {
		if (columns.length != this.columns.length) {
			throw new IllegalArgumentException("Number of columns must match");
		}
		this.columns = Arrays.copyOf(columns, columns.length);
	}

	public DataFrame round(int decimals) {
		double[][] roundedData = Arrays.copyOf(data, data.length);
		NumberFormat df = DecimalFormat.getInstance(Locale.US);//new DecimalFormat("#." + "0".repeat(decimals));

		for (int i = 0; i < roundedData.length; i++) {
			for (int j = 0; j < roundedData[i].length; j++) {
				roundedData[i][j] = Double.parseDouble(df.format(roundedData[i][j]));
			}
		}

		return new DataFrame(roundedData, columns, index);
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("      ");
		for (String column : columns) {
			result.append(String.format("%12s", column));
		}
		result.append("\n");

		for (int i = 0; i < data.length; i++) {
			result.append(String.format("%-6s", indexName != null ? indexName : index[i]));
			for (double value : data[i]) {
				result.append(String.format("%12.3f", value));
			}
			result.append("\n");
		}
		return result.toString();
	}

	public DataFrame tail(int n) {
		if (n > data.length) {
			return this;
		}
		double[][] tailData = Arrays.copyOfRange(data, data.length - n, data.length);
		int[] tailIndex = Arrays.copyOfRange(index, index.length - n, index.length);
		return new DataFrame(tailData, columns, tailIndex);
	}
}
