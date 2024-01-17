package jqwik2.api.database;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class DirectoryBasedFailureDatabase implements FailureDatabase {
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
			} catch (IOException e) {
				ExceptionSupport.throwAsUnchecked(e);
			}
		}
	}

	@Override
	public void saveFailure(String id, SampleRecording recording) {
		try {
			var pathNormalizedId = id;
			Path failureDirectory = databasePath.resolve(pathNormalizedId);
			if (Files.notExists(failureDirectory)) {
				Files.createDirectories(failureDirectory);
			}
			try {
				var sampleId = recording.hashCode();
				Path samplePath = failureDirectory.resolve("sample-" + sampleId);
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
			} catch (Exception e) {
				ExceptionSupport.throwAsUnchecked(e);
			}
		} catch (IOException e) {
			ExceptionSupport.throwAsUnchecked(e);
		}
	}

	@Override
	public void deleteFailure(String propertyId, SampleRecording recording) {

	}

	@Override
	public void deleteProperty(String propertyId) {

	}

	@Override
	public Set<SampleRecording> loadFailures(String propertyId) {
		return Collections.emptySet();
	}

	@Override
	public void clear() {

	}
}
