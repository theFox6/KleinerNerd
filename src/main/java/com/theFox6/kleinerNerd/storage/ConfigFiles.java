package com.theFox6.kleinerNerd.storage;

import foxLog.queued.QueuedLog;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class ConfigFiles {
	private static String token;
	private static Collection<String> owners;
	private static Collection<String> scc;

	/**
	 * Read a file and return it's contents as String.
	 * @param filename the file to be loaded
	 * @return the contents of the file
	 * @throws IOException if anything goes wrong
	 */
	private static String getFileContent(String filename) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}

	/**
	 * Reads a file and returns its contents
	 * @param filename the path to the file to read
	 * @param ignoreEC whether to ignore empty lines and comments
	 * @return a list containing the lines from the file
	 * @throws IOException if something goes wrong while reading the file
	 */
	private static Collection<String> getFileContents(String filename, boolean ignoreEC) throws IOException {
		if (ignoreEC) {
			try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
				return reader.lines().filter(s -> !s.isEmpty()).filter(s -> !s.startsWith("#")).collect(Collectors.toList());
			}
		} else {
			try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
				return reader.lines().collect(Collectors.toList());
			}
		}
	}

	/**
	 * @return the token this bot uses for login
	 */
	public static String getToken() {
		if (token == null) {
			try {
				token = getFileContent("secrets/token.txt");
			} catch (FileNotFoundException e) {
				QueuedLog.error("missing token.txt file in secrets folder");
				return null;
			} catch (IOException e) {
				QueuedLog.error("error while trying to read secrets/token.txt file",e);
				return null;
			}
		}
		return token;
	}
	
	/**
	 * @return the user IDs of the owners of this bot
	 */
	public static Collection<String> getOwners() {
		if (owners == null) {
			try {
				owners = getFileContents("secrets/owners.txt",true);
			} catch (IOException e) {
				QueuedLog.error("error while trying to read secrets/owner.txt resource file",e);
				return new LinkedList<>();
			}
		}
		return owners;
	}
	
	/**
	 * Actually just unloads the owners, so it will be loaded the next time someone asks.
	 */
	public static void reloadOwner() {
		owners = null;
	}

	public static Collection<String> getSuicideChannels() {
		if (scc == null) {
			try {
				scc = getFileContents("secrets/suicide-channels.txt",true);
			} catch (IOException e) {
				QueuedLog.error("error while trying to read secrets/suicide-channels.txt resource file",e);
				return new LinkedList<>();
			}
		}
		return scc;
	}
}
