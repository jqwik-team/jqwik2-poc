package jqwik2.api;

public interface Reporter {

	static String CATEGORY_RESULT = "result";
	static String CATEGORY_PARAMETER = "parameter";

	// void publishNow(String key, String text);

	void appendToReport(String category, String key, Object value);
}
