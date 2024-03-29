package com.theFox6.kleinerNerd.reactionRoles;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.data.MessageLocation;
import com.theFox6.kleinerNerd.storage.StorageManager;
import foxLog.queued.QueuedLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

public class ReactionRoleStorage {
	private static final Path gsf = KleinerNerd.dataFolder.resolve("reactionrole.json");

	//Cannot serialize message location object as map key
	//workaround: serialize as list and load into map
	private static ConcurrentHashMap<MessageLocation,ReactionRoleMap> rrConfig;

	public static void load() {
		LinkedList<ReactionRoleConfig> ser = null;
		if (Files.exists(gsf)) {
			JavaType storedType = TypeFactory.defaultInstance().constructParametricType(LinkedList.class, ReactionRoleConfig.class);
			try {
				ser = new ObjectMapper().readValue(Files.newBufferedReader(gsf), storedType);
			} catch (JsonParseException e) {
				QueuedLog.error("parse error while trying to load guild settings", e);
				StorageManager.loadingError("reactionrole","JSON parse error while loading guild settings");
			} catch (JsonMappingException e) {
				QueuedLog.error("mapping error while trying to load guild settings", e);
				StorageManager.loadingError("reactionrole","JSON mapping error while loading guild settings");
			} catch (IOException e) {
				QueuedLog.error("io error while trying to load guild settings", e);
				StorageManager.loadingError("reactionrole","io error while loading guild settings");
			}
		}
		rrConfig = new ConcurrentHashMap<>();
		ReactionRoleConfig.intoMap(ser, rrConfig);
	}
	
	public static void save() {
		LinkedList<ReactionRoleConfig> ser = ReactionRoleConfig.list(rrConfig);
		StorageManager.save("reactionrole", ser);
	}
	
	public static ReactionRoleMap getConfig(MessageLocation msgLoc) {
		return rrConfig.get(msgLoc);
	}
	
	public static ReactionRoleMap getOrCreateConfig(MessageLocation msgLoc) {
		ReactionRoleMap rrm = rrConfig.get(msgLoc);
		if (rrm == null) {
			rrm = new ReactionRoleMap();
			rrConfig.put(msgLoc, rrm);
		}
		return rrm;
	}

    public static void removeIfEmpty(MessageLocation loc) {
		ReactionRoleMap rrm = rrConfig.get(loc);
		if (rrm == null)
			return;
		if (rrm.isEmpty())
			rrConfig.remove(loc,rrm);
	}
}
