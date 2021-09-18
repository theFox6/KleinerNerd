package com.theFox6.kleinerNerd.storage;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;
import foxLog.queued.QueuedLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StorageManager {
	private static ConcurrentHashMap<String,AtomicBoolean> brokenFiles = new ConcurrentHashMap<>();
	
	public static ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> load(String filename) {
		Path sf = KleinerNerd.dataFolder.resolve(filename + ".json");
		if (Files.exists(sf)) {
			TypeFactory tf = TypeFactory.defaultInstance();
			JavaType dataType = tf.constructMapType(ConcurrentHashMap.class, tf.constructType(String.class), tf.constructMapType(ConcurrentHashMap.class, String.class, AtomicInteger.class));
			try {
				return new ObjectMapper().readValue(Files.newBufferedReader(sf), dataType);
			} catch (JsonParseException e) {
				QueuedLog.error("parse error while trying to load " + filename + "", e);
				loadingError(filename, "JSON parse error while loading " + filename + "");
			} catch (JsonMappingException e) {
				QueuedLog.error("mapping error while trying to load " + filename + "", e);
				loadingError(filename,"JSON mapping error while loading " + filename + "");
			} catch (IOException e) {
				QueuedLog.error("io error while trying to load " + filename + "", e);
				loadingError(filename,"io error while loading " + filename + "");
			}
		}
		return new ConcurrentHashMap<>();
	}

	public static void loadingError(String filename, String message) {
		if (brokenFiles.containsKey(filename))
			brokenFiles.get(filename).set(true);
		else
			brokenFiles.put(filename, new AtomicBoolean(true));
		// TODO go into some error state and start diagnosis
		// probably ask the owner whether to shutdown without quit hooks
	}

	public static <T> void save(String filename,T data) {
		QueuedLog.action("saving " + filename);
		if (!brokenFiles.containsKey(filename))
			brokenFiles.put(filename, new AtomicBoolean(false));
		Path sf = KleinerNerd.dataFolder.resolve(filename + ".json");
		if (brokenFiles.get(filename).get()) {
			Path bf = KleinerNerd.dataFolder.resolve(filename + "_broken.json");
			if (Files.exists(bf)) {
				QueuedLog.error("previous "+filename+" file was broken, not overriding");
				return;
			} else {
				try {
					Files.move(sf, bf);
				} catch (IOException e) {
					QueuedLog.error("failed to rename prevoius (broken) " + filename + " file, aborting save", e);
					return;
				}
				QueuedLog.warning("previous " + filename + " file was broken and renamed to " + bf.toAbsolutePath());
			}
		}
		try {
			new ObjectMapper().writeValue(Files.newBufferedWriter(sf), data);
		} catch (JsonGenerationException e) {
			QueuedLog.error("generation error while trying to save " + filename + "", e);
			saveError(filename,"JSON generation error while writing " + filename + "");
		} catch (JsonMappingException e) {
			QueuedLog.error("mapping error while trying to save " + filename + "", e);
			saveError(filename,"JSON mapping error while writing " + filename + "");
		} catch (IOException e) {
			QueuedLog.error("io error while trying to save " + filename + "", e);
			saveError(filename,"io error while writing " + filename + "");
		}
	}

	private static void saveError(String filename, String msg) {
		// TODO open some idling thread so it doesn't shutdown
		// TODO ask owner whether to shutdown and risk data loss
	}
}
