package com.theFox6.kleinerNerd.storage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;

import foxLog.queued.QueuedLog;

public class StorageManager {
	private static ConcurrentHashMap<String,AtomicBoolean> brokenFiles = new ConcurrentHashMap<>();
	
	public static ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> load(String filename) {
		File sf = new File(KleinerNerd.dataFolder + filename + ".json");
		if (sf.exists()) {
			TypeFactory tf = TypeFactory.defaultInstance();
			JavaType dataType = tf.constructMapType(ConcurrentHashMap.class, tf.constructType(String.class), tf.constructMapType(ConcurrentHashMap.class, String.class, AtomicInteger.class));
			try {
				return new ObjectMapper().readValue(sf, dataType);
			} catch (JsonParseException e) {
				QueuedLog.error("parse error while trying to load guild settings", e);
				loadingError(filename, "JSON parse error while loading guild settings");
			} catch (JsonMappingException e) {
				QueuedLog.error("mapping error while trying to load guild settings", e);
				loadingError(filename,"JSON mapping error while loading guild settings");
			} catch (IOException e) {
				QueuedLog.error("io error while trying to load guild settings", e);
				loadingError(filename,"io error while loading guild settings");
			}
		}
		return new ConcurrentHashMap<>();
	}
	
	private static void loadingError(String filename, String message) {
		if (brokenFiles.containsKey(filename))
			brokenFiles.get(filename).set(true);
		else
			brokenFiles.put(filename, new AtomicBoolean(true));
		// TODO go into some error state and start diagnosis
		// probably ask the owner whether to shutdown without quit hooks
	}

	public static void save(String filename,ConcurrentHashMap<String, ConcurrentHashMap<String, AtomicInteger>> data) {
		if (!brokenFiles.containsKey(filename))
			brokenFiles.put(filename, new AtomicBoolean(false));
		File sf = new File(KleinerNerd.dataFolder + filename + ".json");
		if (brokenFiles.get(filename).get()) {
			if (sf.exists()) {
				QueuedLog.error("previous guild setting file was broken, not overriding");
				return;
			} else {
				File bf = new File(KleinerNerd.dataFolder + filename + "_broken.json");
				if (sf.renameTo(bf)) {
					QueuedLog.warning("previous guild setting file was broken and renamed to " + bf.getAbsolutePath());
				} else {
					QueuedLog.error("failed to rename prevoius (broken) guild setting file, aborting save");
					return;
				}
			}
		}
		try {
			new ObjectMapper().writeValue(sf, data);
		} catch (JsonGenerationException e) {
			QueuedLog.error("generation error while trying to save guild settings", e);
			saveError(filename,"JSON generation error while writing guild settings");
		} catch (JsonMappingException e) {
			QueuedLog.error("mapping error while trying to save guild settings", e);
			saveError(filename,"JSON mapping error while writing guild settings");
		} catch (IOException e) {
			QueuedLog.error("io error while trying to save guild settings", e);
			saveError(filename,"io error while writing guild settings");
		}
	}

	private static void saveError(String filename, String msg) {
		// TODO open some idling thread so it doesn't shutdown
		// TODO ask owner whether to shutdown and risk data loss
	}
}
