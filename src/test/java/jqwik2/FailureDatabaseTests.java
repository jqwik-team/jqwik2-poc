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
		database.clear();
		Files.deleteIfExists(basePath);
	}

	@Example
	void saveAndLoadFailures() {
		var sample1 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2)));
		var sample2 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(3)));
		var sample3 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2), Recording.atom(3)));
		var sample4 = new SampleRecording(List.of(Recording.atom(2), Recording.atom(1)));


		database.saveFailure("id1", sample1);
		database.saveFailure("id1", sample2);
		database.saveFailure("id1", sample3);
		database.saveFailure("id1", sample4);

		Set<SampleRecording> failures = database.loadFailures("id1");

		assertThat(failures).hasSize(4);
		assertThat(failures).contains(sample1, sample2, sample3, sample4);
	}

	@Example
	void loadFailuresIgnoresSamplesThatCannotBeDeserialized() throws IOException {
		var sample1 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2)));
		var sample2 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(3)));
		var sample3 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2), Recording.atom(3)));
		var sample4 = new SampleRecording(List.of(Recording.atom(2), Recording.atom(1)));


		database.saveFailure("id1", sample1);
		database.saveFailure("id1", sample2);
		database.saveFailure("id1", sample3);
		database.saveFailure("id1", sample4);

		var sample4File = basePath.resolve("id1").resolve("sample-" + sample4.hashCode());
		assertThat(sample4File).exists();
		Files.writeString(sample4File, "cannot:be:deserialized", StandardOpenOption.TRUNCATE_EXISTING);

		Set<SampleRecording> failures = database.loadFailures("id1");
		assertThat(failures).hasSize(3);
		assertThat(failures).contains(sample1, sample2, sample3);
	}

	@Example
	void saveAndDeleteFailure() {
		var sample1 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2)));
		var sample2 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(3)));
		var sample4 = new SampleRecording(List.of(Recording.atom(2), Recording.atom(1)));

		database.saveFailure("id1", sample1);
		database.saveFailure("id1", sample2);

		database.deleteFailure("id1", sample1);
		Set<SampleRecording> failures = database.loadFailures("id1");
		assertThat(failures).hasSize(1);
		assertThat(failures).contains(sample2);

		database.deleteFailure("id1", sample1); // idempotent
		database.deleteFailure("id1", sample4); // non existant failure
	}

	@Example
	void deleteProperty() {
		var sample1 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2)));
		var sample2 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(3)));

		database.saveFailure("id1", sample1);
		database.saveFailure("id1", sample2);

		database.deleteProperty("id1");
		Set<SampleRecording> failures = database.loadFailures("id1");
		assertThat(failures).isEmpty();

		database.deleteProperty("id1"); // idempotent
		database.deleteProperty("id2"); // non existant id
	}

	@Example
	void clear() {
		var sample1 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2)));
		var sample2 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(3)));

		database.saveFailure("id1", sample1);
		database.saveFailure("id2", sample2);

		database.clear();
		assertThat(database.loadFailures("id1")).isEmpty();
		assertThat(database.loadFailures("id2")).isEmpty();
		assertThat(database.failingProperties()).isEmpty();
	}

	@Example
	void propertyIds() {
		var sample1 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(2)));
		var sample2 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(3)));
		var sample3 = new SampleRecording(List.of(Recording.atom(1), Recording.atom(4)));

		database.saveFailure("id1", sample1);
		database.saveFailure("id2", sample2);
		database.saveFailure("id3 and some", sample3);

		database.failingProperties();
		assertThat(database.failingProperties())
			.containsExactlyInAnyOrder("id1", "id2", "id3 and some");
	}
}