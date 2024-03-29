package jqwik2;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import jqwik2.api.database.*;
import jqwik2.api.recording.*;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import net.jqwik.api.lifecycle.*;

import static jqwik2.api.recording.Recording.*;
import static org.assertj.core.api.Assertions.*;

class FailureDatabaseTests {

	Path basePath;
	DirectoryBasedFailureDatabase database;

	@BeforeExample
	void setupDatabase() throws IOException {
		basePath = Files.createTempDirectory("jqwik-failures").toAbsolutePath();
		database = new DirectoryBasedFailureDatabase(basePath);
	}

	@BeforeTry
	void clearDatabase() {
		database.clear();
	}

	@AfterExample
	void deleteDatabase() throws IOException {
		database.clear();
		Files.deleteIfExists(basePath);
	}

	@Example
	void saveAndLoadFailures() {
		var sample1 = new SampleRecording(List.of(choice(1), choice(2)));
		var sample2 = new SampleRecording(List.of(choice(1), choice(3)));
		var sample3 = new SampleRecording(List.of(choice(1), choice(2), choice(3)));
		var sample4 = new SampleRecording(List.of(choice(2), choice(1)));


		database.saveFailingSample("id1", sample1);
		database.saveFailingSample("id1", sample2);
		database.saveFailingSample("id1", sample3);
		database.saveFailingSample("id1", sample4);

		Set<SampleRecording> failures = database.loadFailingSamples("id1");

		assertThat(failures).hasSize(4);
		assertThat(failures).contains(sample1, sample2, sample3, sample4);
	}

	@Property(tries = 10, shrinking = ShrinkingMode.FULL)
	void makeSureSampleIdDoesNotCollideWithLongRecordingsAndJustOneChange(@ForAll @Size(min = 1, max = 5000) List<@IntRange(min = 0) Integer> atoms) {
		List<Recording> longListOfAtoms = atoms.stream().map(choice -> (Recording) choice(choice)).toList();
		ArrayList<Recording> longListOfAtoms2 = new ArrayList<>(longListOfAtoms);
		longListOfAtoms2.set(longListOfAtoms2.size() - 1, choice(99));
		var sample1 = new SampleRecording(longListOfAtoms);
		var sample2 = new SampleRecording(longListOfAtoms2);

		database.saveFailingSample("id1", sample1);
		database.saveFailingSample("id1", sample2);

		Set<SampleRecording> failures = database.loadFailingSamples("id1");
		assertThat(failures).containsExactlyInAnyOrder(sample1, sample2);
	}

	@Example
	void loadFailuresIgnoresSamplesThatCannotBeDeserialized() throws IOException {
		var sample1 = new SampleRecording(List.of(choice(1), choice(2)));
		var sample2 = new SampleRecording(List.of(choice(1), choice(3)));
		var sample3 = new SampleRecording(List.of(choice(1), choice(2), choice(3)));
		var sample4 = new SampleRecording(List.of(choice(2), choice(1)));


		database.saveFailingSample("id1", sample1);
		database.saveFailingSample("id1", sample2);
		database.saveFailingSample("id1", sample3);
		database.saveFailingSample("id1", sample4);

		var sample4File = basePath.resolve("id1").resolve(DirectoryBasedFailureDatabase.SAMPLEFILE_PREFIX + sample4.hashCode());
		assertThat(sample4File).exists();
		Files.writeString(sample4File, "cannot:be:deserialized", StandardOpenOption.TRUNCATE_EXISTING);

		Set<SampleRecording> failures = database.loadFailingSamples("id1");
		assertThat(failures).hasSize(3);
		assertThat(failures).contains(sample1, sample2, sample3);
	}

	@Example
	void saveAndDeleteFailure() {
		var sample1 = new SampleRecording(List.of(choice(1), choice(2)));
		var sample2 = new SampleRecording(List.of(choice(1), choice(3)));
		var sample4 = new SampleRecording(List.of(choice(2), choice(1)));

		database.saveFailingSample("id1", sample1);
		database.saveFailingSample("id1", sample2);

		database.deleteFailure("id1", sample1);
		Set<SampleRecording> failures = database.loadFailingSamples("id1");
		assertThat(failures).hasSize(1);
		assertThat(failures).contains(sample2);

		database.deleteFailure("id1", sample1); // idempotent
		database.deleteFailure("id1", sample4); // non existant failure
	}

	@Example
	void deleteProperty() {
		var sample1 = new SampleRecording(List.of(choice(1), choice(2)));
		var sample2 = new SampleRecording(List.of(choice(1), choice(3)));

		database.saveFailingSample("id1", sample1);
		database.saveFailingSample("id1", sample2);
		assertThat(database.hasFailed("id1")).isTrue();

		database.deleteProperty("id1");
		assertThat(database.hasFailed("id1")).isFalse();
		assertThat(database.loadFailingSamples("id1")).isEmpty();

		database.deleteProperty("id1"); // idempotent
		database.deleteProperty("id2"); // non existant id

	}

	@Example
	void clear() {
		var sample1 = new SampleRecording(List.of(choice(1), choice(2)));
		var sample2 = new SampleRecording(List.of(choice(1), choice(3)));

		database.saveFailingSample("id1", sample1);
		database.saveFailingSample("id2", sample2);

		database.clear();
		assertThat(database.loadFailingSamples("id1")).isEmpty();
		assertThat(database.loadFailingSamples("id2")).isEmpty();
		assertThat(database.failingProperties()).isEmpty();
	}

	@Example
	void saveAndLoadSeed() {
		var nonExistingSeed = database.loadSeed("id1");
		assertThat(nonExistingSeed).isEmpty();

		database.saveSeed("id1", "1234567890");
		var seed = database.loadSeed("id1");
		assertThat(seed).hasValue("1234567890");
	}

	@Example
	void saveFailureInOneGo() {
		var sample1 = new SampleRecording(List.of(choice(1), choice(2)));
		var sample2 = new SampleRecording(List.of(choice(1), choice(3)));
		Set<SampleRecording> failedSamples = Set.of(sample1, sample2);
		database.saveFailure("id1", "1234567890", failedSamples);

		assertThat(database.loadSeed("id1")).hasValue("1234567890");
		assertThat(database.loadFailingSamples("id1")).containsExactlyInAnyOrder(sample1, sample2);
	}

	@Example
	void propertyIds() {
		var sample1 = new SampleRecording(List.of(choice(1), choice(2)));
		var sample2 = new SampleRecording(List.of(choice(1), choice(3)));
		var sample3 = new SampleRecording(List.of(choice(1), choice(4)));
		var sample4 = new SampleRecording(List.of(choice(1), choice(5)));

		database.saveFailingSample("id1", sample1);
		database.saveFailingSample("id2", sample2);
		database.saveFailingSample("id3 and some", sample3);
		database.saveFailingSample(".id4", sample4);

		database.failingProperties();
		assertThat(database.failingProperties())
			.containsExactlyInAnyOrder("id1", "id2", "id3 and some", ".id4");
	}

	@Example
	void performanceTestForDatabaseOverhead(
		@ForAll @Size(200) List<@AlphaChars @Chars({' ', ':', '#'}) @StringLength(min = 1, max = 20) String> propertyIds,
		@ForAll @Size(200) List<@NumericChars @StringLength(min = 1, max = 20) String> seeds
	) throws Exception {
		AtomicInteger next = new AtomicInteger(0);
		var repeats = 200;
		PerformanceTesting.time("load property with seed and failures", repeats, () -> {
			String propertyId = propertyIds.get(next.get());
			String seed = seeds.get(next.getAndIncrement());

			database.saveFailure(propertyId, seed, Set.of(
				new SampleRecording(List.of(choice(1), choice(2))),
				new SampleRecording(List.of(choice(3), choice(4)))
			));

			// Checking the values uses most of the time
			// assertThat(database.loadSeed(propertyId)).hasValue(seed);
			// assertThat(database.loadFailingSamples(propertyId)).hasSize(2);
		});
	}
}
