package jqwik2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import jqwik2.api.database.*;
import jqwik2.api.recording.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

import static org.assertj.core.api.Assertions.*;

class FailureDatabaseTests {

	Path basePath;
	DirectoryBasedFailureDatabase database;

	@BeforeExample
	void setupDatabase() throws IOException {
		basePath = Files.createTempDirectory("jqwik-failures").toAbsolutePath();
		System.out.println("Created database directory " + basePath);
		database = new DirectoryBasedFailureDatabase(basePath);
	}

	@AfterExample
	void deleteDatabase() throws IOException {
		Files.list(basePath).forEach(failureDirectory -> {
			try {
				System.out.println("> " + failureDirectory.getFileName() + " " + Files.isDirectory(failureDirectory));
				Files.list(failureDirectory).forEach(samplePath -> {
					System.out.println(">> " + samplePath.getFileName());
					try {
						Files.lines(samplePath).forEach(System.out::println);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
				// Files.deleteIfExists(failureDirectory);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});

		System.out.println(basePath);
		// Files.deleteIfExists(basePath);
	}

	@Example
	void saveFailures() {

		var sample1 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2)));
		var sample2 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(3)));
		var sample3 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2), Recording.atom(3)));
		var sample4 = new SampleRecording(List.of(Recording.atom(2), Recording.atom(1)));


		database.saveFailure("id1", sample1);
		database.saveFailure("id1", sample2);
		database.saveFailure("id1", sample3);
		database.saveFailure("id1", sample4);

		Set<SampleRecording> failures = database.loadFailures("id1");

		// assertThat(failures).hasSize(4);
		// assertThat(failures).contains(sample1, sample2, sample3, sample4);
	}
}
