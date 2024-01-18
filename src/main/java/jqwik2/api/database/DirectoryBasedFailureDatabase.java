package jqwik2.api.database;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class DirectoryBasedFailureDatabase implements FailureDatabase {
	public static final String SAMPLEFILE_PREFIX = "sample-";
	private final Path databasePath;

	public DirectoryBasedFailureDatabase(Path databasePath) {
		this.databasePath = databasePath;
		ensureDatabasePathExists();
	}

	private void ensureDatabasePathExists() {
		if (Files.notExists(databasePath)) {
			try {
				Files.createDirectories(databasePath);
				System.out.println("Created database directory " + databasePath);
				if (!databasePath.toFile().canWrite()) {
					throw new IOException("Cannot write to " + databasePath);
				}
			} catch (IOException e) {
				ExceptionSupport.throwAsUnchecked(e);
			}
		}
	}

	@Override
	public void saveFailure(String id, SampleRecording recording) {
		try {
			var propertyDirectory = propertyDirectory(id, true);
			var samplePath = samplePath(recording, propertyDirectory);
			if (Files.notExists(samplePath)) {
				var sampleFile = Files.createFile(samplePath);
				try (var writer = Files.newBufferedWriter(sampleFile)) {
					try {
						writer.write(recording.serialize());
					} catch (IOException e) {
						ExceptionSupport.throwAsUnchecked(e);
					}
				}
			}
		} catch (IOException e) {
			ExceptionSupport.throwAsUnchecked(e);
		}
	}

	private static Path samplePath(SampleRecording recording, Path propertyDirectory) {
		var sampleId = recording.hashCode();
		return propertyDirectory.resolve("sample-" + sampleId);
	}

	private Path propertyDirectory(String id, boolean createIfNecessary) throws IOException {
		var idBasedFileName = toFileName(id);
		Path propertyDirectory = databasePath.resolve(idBasedFileName);
		if (createIfNecessary && Files.notExists(propertyDirectory)) {
			Files.createDirectories(propertyDirectory);
		}
		return propertyDirectory;
	}

	private String toFileName(String id) {
		// Replace spaces with underscores. Remove all other non-alphanumeric characters.
		return id.replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9_]", "");
	}

	@Override
	public Set<SampleRecording> loadFailures(String propertyId) {
		try {
			var propertyDirectory = propertyDirectory(propertyId, false);
			if (Files.notExists(propertyDirectory)) {
				return Collections.emptySet();
			}
			return Files.list(propertyDirectory)
						.filter(Files::isRegularFile)
						.filter(path -> path.getFileName().toString().startsWith(SAMPLEFILE_PREFIX))
						.toList().stream().map(this::loadSample).collect(Collectors.toSet());
		} catch (IOException e) {
			return ExceptionSupport.throwAsUnchecked(e);
		}
	}

	private SampleRecording loadSample(Path path) {
		try {
			var lines = Files.readAllLines(path);
			var serialized = String.join("", lines);
			return SampleRecording.deserialize(serialized);
		} catch (IOException e) {
			return ExceptionSupport.throwAsUnchecked(e);
		}
	}

	@Override
	public void deleteFailure(String propertyId, SampleRecording recording) {

	}

	@Override
	public void deleteProperty(String propertyId) {

	}

	@Override
	public void clear() {
		try {
			deleteAllIn(databasePath);
		} catch (IOException e) {
			ExceptionSupport.throwAsUnchecked(e);
		}
	}

	private void deleteAllIn(Path path) throws IOException {
		Files.walk(path)
			 .sorted(Comparator.reverseOrder())
			 .map(Path::toFile)
			 .forEach(File::delete);
	}
}
