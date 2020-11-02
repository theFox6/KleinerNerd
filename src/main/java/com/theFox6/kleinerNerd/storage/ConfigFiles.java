package com.theFox6.kleinerNerd.storage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;

import foxLog.queued.QueuedLog;

public class ConfigFiles {
	private static String token;
	private static String owner;
	
	/**
	 * Read a file and return it's contents as String.
	 * @param filename the file to be loaded
	 * @return the contents of the file
	 * @throws IOException
	 */
	private static String getFileContent(String filename) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
			return reader.lines().collect(Collectors.joining("\n"));
		}
	}
		
	/**
	 * @return the token this bot uses for login
	 */
	public static String getToken() {
		if (token == null) {
			try {
				token = getFileContent("secrets/token.txt");
			} catch (IOException e) {
				QueuedLog.error("error while trying to read secrets/token.txt file");
				return null;
			}
		}
		return token;
	}
	
	/**
	 * @return the user ID of the owner of this bot
	 */
	public static String getOwner() {
		if (owner == null) {
			try {
				owner = getFileContent("secrets/owner.txt");
			} catch (IOException e) {
				QueuedLog.error("error while trying to read secrets/owner.txt resource file");
				return null;
			}
		}
		return owner;
	}
	
	/**
	 * Actually just unloads the owner, so it will be loaded the next time someone asks.
	 */
	public static void reloadOwner() {
		owner = null;
	}
}
