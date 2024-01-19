package jqwik2.api.database;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class DirectoryBasedFailureDatabase implements FailureDatabase {
	public static final String SAMPLEFILE_PREFIX = "sample#";
	public static final String IDFILE_NAME = "ID";
	public static final String SEEDFILE_NAME = "seed";
	private final Path databasePath;

	public DirectoryBasedFailureDatabase(Path databasePath) {
		this.databasePath = databasePath;
		ensureDatabasePathExists();
	}

	private void ensureDatabasePathExists() {
		if (Files.notExists(databasePath)) {
			try {
				Files.createDirectories(databasePath);
				// System.out.println("Created database directory " + databasePath);
				if (!databasePath.toFile().canWrite()) {
					throw new IOException("Cannot write to " + databasePath);
				}
			} catch (IOException e) {
				ExceptionSupport.throwAsUnchecked(e);
			}
		}
	}

	@Override
	public void saveFailingSample(String propertyId, SampleRecording recording) {
		ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, true);
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
		});
	}

	private static Path samplePath(SampleRecording recording, Path propertyDirectory) {
		var sampleId = recording.hashCode();
		return propertyDirectory.resolve(SAMPLEFILE_PREFIX + sampleId);
	}

	private Path propertyDirectory(String id, boolean createIfNecessary) throws IOException {
		var idBasedFileName = toFileName(id);
		Path propertyDirectory = databasePath.resolve(idBasedFileName);
		if (createIfNecessary && Files.notExists(propertyDirectory)) {
			Files.createDirectories(propertyDirectory);
			createIdFile(id, idBasedFileName, propertyDirectory);
		}
		return propertyDirectory;
	}

	private static void createIdFile(String id, String idBasedFileName, Path propertyDirectory) throws IOException {
		if (!idBasedFileName.equals(id)) {
			var idFile = propertyDirectory.resolve(IDFILE_NAME);
			Files.write(idFile, id.getBytes());
		}
	}

	private static String propertyId(Path propertyDirectory) {
		return ExceptionSupport.runUnchecked(() -> {
			var idFile = propertyDirectory.resolve(IDFILE_NAME);
			if (Files.notExists(idFile)) {
				return propertyDirectory.getFileName().toString();
			} else {
				return new String(Files.readAllBytes(idFile));
			}
		});
	}

	private String toFileName(String id) {
		// Replace spaces with underscores. Remove all other non-alphanumeric characters.
		return id.replaceAll("\\s", "_").replaceAll("[^a-zA-Z0-9_]", "");
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
		return propertyDirectory.resolve(SEEDFILE_NAME);
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
		ExceptionSupport.runUnchecked(() -> deleteAllIn(databasePath));
	}

	@Override
	public Set<String> failingProperties() {
		return ExceptionSupport.runUnchecked(
			() -> Files.list(databasePath)
					   .filter(Files::isDirectory)
					   .map(path -> propertyId(path))
					   .collect(Collectors.toSet()));
	}

	private void deleteAllIn(Path path) throws IOException {
		Files.walk(path)
			 .sorted(Comparator.reverseOrder())
			 .filter(p -> !p.equals(path))
			 .map(Path::toFile)
			 .forEach(File::delete);
	}
}
