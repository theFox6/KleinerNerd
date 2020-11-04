package com.theFox6.kleinerNerd.reactionRoles;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.data.MessageLocation;
import foxLog.queued.QueuedLog;

public class ReactionRoleStorage {
	private static final File gsf = new File(KleinerNerd.dataFolder + "reactionrole.json");
	private static final File bsf = new File(KleinerNerd.dataFolder + "reactionrole_broken.json");
	private static boolean broken = false;
	private static ConcurrentHashMap<MessageLocation,ReactionRoleConfiguration> rrConfig;

	public static void load() {
		if (gsf.exists()) {
			//FIXME: Cannot serialize message location object as map key
			//TODO: serialize as list and load into map or something
			JavaType settingsType = TypeFactory.defaultInstance().constructParametricType(ConcurrentHashMap.class, MessageLocation.class, ReactionRoleConfiguration.class);
			try {
				rrConfig = new ObjectMapper().readValue(gsf, settingsType);
			} catch (JsonParseException e) {
				broken = true;
				QueuedLog.error("parse error while trying to load guild settings", e);
				loadingError("JSON parse error while loading guild settings");
			} catch (JsonMappingException e) {
				broken = true;
				QueuedLog.error("mapping error while trying to load guild settings", e);
				loadingError("JSON mapping error while loading guild settings");
			} catch (IOException e) {
				broken = true;
				QueuedLog.error("io error while trying to load guild settings", e);
				loadingError("io error while loading guild settings");
			}
		}
		if (rrConfig == null)
			rrConfig = new ConcurrentHashMap<>();
	}
	
	public static void save() {
		if (broken) {
			if (bsf.exists()) {
				QueuedLog.error("previous guild setting file was broken, not overriding");
				return;
			} else {
				if (gsf.renameTo(bsf)) {
					QueuedLog.warning("previous guild setting file was broken and renamed to " + bsf.getAbsolutePath());
				} else {
					QueuedLog.error("failed to rename prevoius (broken) guild setting file, aborting save");
					return;
				}
			}
		}
		try {
			new ObjectMapper().writeValue(gsf, rrConfig);
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
	
	public static ReactionRoleConfiguration getConfig(MessageLocation msgLoc) {
		return rrConfig.get(msgLoc);
	}

	private static void loadingError(String msg) {
		// TODO Ask the owner whether to shutdown without quit hooks
	}

	private static void saveError(String msg) {
		// TODO open some idling thread so it doesn't shutdown
		// TODO ask owner whether to shutdown and risk data loss
	}
	
}
