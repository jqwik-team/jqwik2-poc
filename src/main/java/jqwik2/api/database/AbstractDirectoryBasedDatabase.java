package jqwik2.api.database;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class AbstractDirectoryBasedDatabase {
	public static final String ID_FILE_NAME = "ID";

	protected final Path databasePath;

	public AbstractDirectoryBasedDatabase(Path databasePath) {
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

	protected static String toFileName(String id) {
		// Replace spaces with underscores. Remove all disallowed characters. Remove all leading dots
		var disallowedCharsRegex = "[^a-zA-Z0-9_.#$\\-]";
		return id.replaceAll("\\s", "_")
				 .replaceAll(disallowedCharsRegex, "")
				 .replaceAll("^\\.", "");
	}

	protected static void deleteAllIn(Path path) throws IOException {
		Files.walk(path)
			 .sorted(Comparator.reverseOrder())
			 .filter(p -> !p.equals(path))
			 .forEach(p -> {
				 try {
					 Files.delete(p);
				 } catch (IOException e) {
					 ExceptionSupport.throwAsUnchecked(e);
				 }
			 });
	}

	protected Path propertyDirectory(String id, boolean createIfNecessary) throws IOException {
		var idBasedFileName = toFileName(id);
		Path propertyDirectory = databasePath.resolve(idBasedFileName);
		if (createIfNecessary && Files.notExists(propertyDirectory)) {
			Files.createDirectories(propertyDirectory);
			createIdFile(id, propertyDirectory);
		}
		return propertyDirectory;
	}

	private static void createIdFile(String id, Path propertyDirectory) throws IOException {
		var idFile = propertyDirectory.resolve(ID_FILE_NAME);
		Files.write(idFile, id.getBytes());
	}

	protected void deleteAllDirectoriesAndFiles() {
		ExceptionSupport.runUnchecked(() -> deleteAllIn(databasePath));
	}

}
