package jqwik2;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import jqwik2.api.*;
import jqwik2.api.database.*;
import jqwik2.api.recording.*;

import net.jqwik.api.*;
import net.jqwik.api.lifecycle.*;

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
		Files.delete(basePath);
	}

	@Example
	void saveFailures() {

		Set<List<Recording>> samples = Set.of(
			List.of(Recording.atom(1), Recording.atom(2))
		);

		FailureDatabase.PropertyFailure failure = new FailureDatabase.PropertyFailure(
			PropertyRunResult.Status.FAILED,
			samples
		);

		database.saveFailure("id1", failure);

		// Optional<FailureDatabase.PropertyFailure> loadedFailure = database.loadFailure("id1");
		// assertThat(loadedFailure).isPresent();
		// loadedFailure.ifPresent(f -> {
		// 	assertThat(f.status()).isEqualTo(PropertyRunResult.Status.FAILED);
		// 	assertThat(f.falsifiedSamples()).isEqualTo(samples);
		// });


	}
}
