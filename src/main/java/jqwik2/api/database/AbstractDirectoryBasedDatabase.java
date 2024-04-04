package jqwik2.api.database;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

import jqwik2.api.recording.*;
import jqwik2.api.support.*;

public class AbstractDirectoryBasedDatabase {
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

}
