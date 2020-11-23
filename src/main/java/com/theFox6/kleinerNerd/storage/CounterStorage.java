package com.theFox6.kleinerNerd.storage;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;

import foxLog.queued.QueuedLog;

public class CounterStorage {
	private static final File sf = new File(KleinerNerd.dataFolder + "counters.json");
	private static final File bf = new File(KleinerNerd.dataFolder + "counters_broken.json");
	private static boolean isBroken = false;
	//TODO: perhaps use AtomicInteger and serialize as Iteger
	private static ConcurrentHashMap<String,AtomicInteger> settings = null;
	
	public static void load() {
		if (sf.exists()) {
			JavaType settingsType = TypeFactory.defaultInstance().constructParametricType(ConcurrentHashMap.class, String.class, AtomicInteger.class);
			try {
				settings = new ObjectMapper().readValue(sf, settingsType);
			} catch (JsonParseException e) {
				isBroken = true;
				QueuedLog.error("parse error while trying to load guild settings", e);
				loadingError("JSON parse error while loading guild settings");
			} catch (JsonMappingException e) {
				isBroken = true;
				QueuedLog.error("mapping error while trying to load guild settings", e);
				loadingError("JSON mapping error while loading guild settings");
			} catch (IOException e) {
				isBroken = true;
				QueuedLog.error("io error while trying to load guild settings", e);
				loadingError("io error while loading guild settings");
			}
		}
		if (settings == null)
			settings = new ConcurrentHashMap<>();
	}
	
	public static void save() {
		if (isBroken) {
			if (sf.exists()) {
				QueuedLog.error("previous guild setting file was broken, not overriding");
				return;
			} else {
				if (sf.renameTo(bf)) {
					QueuedLog.warning("previous guild setting file was broken and renamed to " + bf.getAbsolutePath());
				} else {
					QueuedLog.error("failed to rename prevoius (broken) guild setting file, aborting save");
					return;
				}
			}
		}
		try {
			new ObjectMapper().writeValue(sf, settings);
		} catch (JsonGenerationException e) {
			QueuedLog.error("generation error while trying to save guild settings", e);
			saveError("JSON generation error while writing guild settings");
		} catch (JsonMappingException e) {
			QueuedLog.error("mapping error while trying to save guild settings", e);
			saveError("JSON mapping error while writing guild settings");
		} catch (IOException e) {
			QueuedLog.error("io error while trying to save guild settings", e);
			saveError("io error while writing guild settings");
		}
	}
	
	public static AtomicInteger getCounter(String key) {
		if (!settings.containsKey(key))
			settings.put(key, new AtomicInteger(0));
		return settings.get(key);
	}
	
	private static void loadingError(String msg) {
		// TODO Ask the owner whether to shutdown without quit hooks
	}

	private static void saveError(String msg) {
		// TODO open some idling thread so it doesn't shutdown
		// TODO ask owner whether to shutdown and risk data loss
	}
}
