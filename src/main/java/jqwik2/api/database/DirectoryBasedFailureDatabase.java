package jqwik2.api.database;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class DirectoryBasedFailureDatabase extends AbstractDirectoryBasedDatabase implements FailureDatabase {
	public static final String SAMPLEFILE_PREFIX = "sample#";
	public static final String SEED_FILE_NAME = "seed";

	public DirectoryBasedFailureDatabase(Path databasePath) {
		super(databasePath);
	}

	@Override
	public void saveFailingSample(String propertyId, SampleRecording recording) {
		ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, true);
			saveSampleIn(recording, propertyDirectory);
		});
	}

	private static Path samplePath(SampleRecording recording, Path propertyDirectory) {
		var sampleId = recording.hashCode();
		return propertyDirectory.resolve(SAMPLEFILE_PREFIX + sampleId);
	}

	private static String propertyId(Path propertyDirectory) {
		return ExceptionSupport.runUnchecked(() -> {
			var idFile = propertyDirectory.resolve(ID_FILE_NAME);
			if (Files.notExists(idFile)) { // The id file got lost
				return propertyDirectory.getFileName().toString();
			} else {
				return new String(Files.readAllBytes(idFile));
			}
		});
	}

	@Override
	public Set<SampleRecording> loadFailingSamples(String propertyId) {
		return ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, false);
			if (Files.notExists(propertyDirectory)) {
				return Collections.emptySet();
			}
			return Files.list(propertyDirectory)
						.filter(Files::isRegularFile)
						.filter(path -> path.getFileName().toString().startsWith(SAMPLEFILE_PREFIX))
						.map(this::loadSample)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(Collectors.toSet());

		});
	}

	private Optional<SampleRecording> loadSample(Path path) {
		return ExceptionSupport.runUnchecked(() -> {
			var lines = Files.readAllLines(path);
			var serialized = String.join("", lines);
			try {
				return Optional.of(SampleRecording.deserialize(serialized));
			} catch (Exception e) {
				System.out.println("Could not deserialize sample recording: " + serialized);
				return Optional.empty();
			}
		});
	}

	@Override
	public void deleteFailure(String propertyId, SampleRecording recording) {
		ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, false);
			if (Files.notExists(propertyDirectory)) {
				return;
			}
			var samplePath = samplePath(recording, propertyDirectory);
			if (Files.exists(samplePath)) {
				Files.delete(samplePath);
			}
		});
	}

	@Override
	public void saveSeed(String propertyId, String seed) {
		ExceptionSupport.runUnchecked(() -> {
			var seedFile = seedFile(propertyId);
			if (seed == null) {
				Files.deleteIfExists(seedFile);
				return;
			}
			Files.writeString(seedFile, seed, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		});
	}

	private Path seedFile(String propertyId) throws IOException {
		var propertyDirectory = propertyDirectory(propertyId, true);
		return propertyDirectory.resolve(SEED_FILE_NAME);
	}

	@Override
	public Optional<String> loadSeed(String propertyId) {
		return ExceptionSupport.runUnchecked(() -> {
			var seedFile = seedFile(propertyId);
			if (Files.notExists(seedFile)) {
				return Optional.empty();
			}
			return Optional.of(Files.readString(seedFile));
		});
	}

	@Override
	public boolean hasFailed(String propertyId) {
		return ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, false);
			return Files.exists(propertyDirectory);
		});
	}

	@Override
	public void deleteProperty(String propertyId) {
		ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, false);
			if (Files.notExists(propertyDirectory)) {
				return;
			}
			deleteAllIn(propertyDirectory);
			Files.delete(propertyDirectory);
		});
	}

	@Override
	public void clear() {
		deleteAllDirectoriesAndFiles();
	}

	@Override
	public Set<String> failingProperties() {
		return ExceptionSupport.runUnchecked(
			() -> Files.list(databasePath)
					   .filter(Files::isDirectory)
					   .map(path -> propertyId(path))
					   .collect(Collectors.toSet()));
	}

	@Override
	public void saveFailure(String propertyId, String seed, Set<SampleRecording> failingSamples) {
		// Optimized version
		ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, true);
			deleteAllSamples(propertyDirectory);
			if (seed != null) {
				var seedFile = seedFile(propertyId);
				Files.writeString(seedFile, seed, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			}
			for (SampleRecording sample : failingSamples) {
				saveSampleIn(sample, propertyDirectory);
			}
		});
	}

	private static void deleteAllSamples(Path propertyDirectory) throws IOException {
		Files.walk(propertyDirectory)
			 .filter(p -> p.getFileName().toString().startsWith(SAMPLEFILE_PREFIX))
			 .forEach(p -> {
				 try {
					 Files.delete(p);
				 } catch (IOException e) {
					 ExceptionSupport.throwAsUnchecked(e);
				 }
			 });
	}

	private static void saveSampleIn(SampleRecording sample, Path propertyDirectory) throws IOException {
		var samplePath = samplePath(sample, propertyDirectory);
		if (Files.notExists(samplePath)) {
			var sampleFile = Files.createFile(samplePath);
			try (var writer = Files.newBufferedWriter(sampleFile)) {
				try {
					writer.write(sample.serialize());
				} catch (IOException e) {
					ExceptionSupport.throwAsUnchecked(e);
				}
			}
		}
	}
}
