package jqwik2.api.database;

import java.io.*;
import java.nio.file.*;
import java.util.*;

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
	public void saveFailure(String id, PropertyFailure failure) {

	}

	@Override
	public void deleteFailure(String id, PropertyFailure failure) {

	}

	@Override
	public Optional<PropertyFailure> loadFailure(String id) {
		return Optional.empty();
	}

	@Override
	public void clear() {

	}
}
