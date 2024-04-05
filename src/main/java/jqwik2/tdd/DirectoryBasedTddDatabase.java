package jqwik2.tdd;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import jqwik2.api.database.*;
import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class DirectoryBasedTddDatabase extends AbstractDirectoryBasedDatabase implements TddDatabase {

	public static final String CASE_PREFIX = "tdd#";

	public DirectoryBasedTddDatabase(Path databasePath) {
		super(databasePath);
	}

	@Override
	public void saveSample(String propertyId, String caseLabel, SampleRecording recording) {
		ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, true);
			saveSampleToCase(recording, caseLabel, propertyDirectory);
		});
	}

	private void saveSampleToCase(SampleRecording recording, String caseLabel, Path propertyDirectory) throws IOException {
		Path caseFile = caseFile(caseLabel, propertyDirectory);
		var recordings = readSampleRecordings(caseFile);
		if (!recordings.contains(recording)) {
			try (var writer = Files.newBufferedWriter(caseFile, StandardOpenOption.APPEND)) {
				writer.write(recording.serialize());
				writer.newLine();
			}
		}
	}

	private Path caseFile(String caseLabel, Path propertyDirectory) throws IOException {
		var caseLabelFileName = CASE_PREFIX + toFileName(caseLabel);
		Path caseFile = propertyDirectory.resolve(caseLabelFileName);
		if (Files.notExists(caseFile)) {
			Files.createFile(caseFile);
		}
		return caseFile;
	}

	@Override
	public Set<SampleRecording> loadSamples(String propertyId, String caseLabel) {
		return ExceptionSupport.runUnchecked(() -> {
			var propertyDirectory = propertyDirectory(propertyId, false);
			if (Files.notExists(propertyDirectory)) {
				return Collections.emptySet();
			}
			var caseFile = caseFile(caseLabel, propertyDirectory);
			return readSampleRecordings(caseFile);
		});
	}

	private static Set<SampleRecording> readSampleRecordings(Path caseFile) {
		// TODO: Throws java.nio.channels.ClosedByInterruptException when tests are run together
		try (var lines = Files.lines(caseFile)) {
			return lines
					   .filter(line -> !line.isBlank())
					   .map(SampleRecording::deserialize)
					   .collect(Collectors.toSet());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void clear() {
		deleteAllDirectoriesAndFiles();
	}
}
