package com.theFox6.kleinerNerd.categories;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.storage.StorageManager;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Guild;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CategoryStorage {
	private static ConcurrentHashMap<String,ConcurrentHashMap<String,CategoryLayout>> categories = null;
	
	public static void load() {
		Path sf = KleinerNerd.dataFolder.resolve("category_configurations.json");
		if (Files.exists(sf)) {
			TypeFactory tf = TypeFactory.defaultInstance();
			JavaType dataType = tf.constructMapType(ConcurrentHashMap.class, tf.constructType(String.class), tf.constructMapType(ConcurrentHashMap.class, String.class, CategoryLayout.class));
			try {
				categories = new ObjectMapper().readValue(Files.newBufferedReader(sf), dataType);
				return;
			} catch (JsonParseException e) {
				QueuedLog.error("parse error while trying to load guild settings", e);
				StorageManager.loadingError("category_configurations", "JSON parse error while loading guild settings");
			} catch (JsonMappingException e) {
				QueuedLog.error("mapping error while trying to load guild settings", e);
				StorageManager.loadingError("category_configurations","JSON mapping error while loading guild settings");
			} catch (IOException e) {
				QueuedLog.error("io error while trying to load guild settings", e);
				StorageManager.loadingError("category_configurations","io error while loading guild settings");
			}
		}
		categories = new ConcurrentHashMap<>();
	}
	
	public static void save() {
		StorageManager.save("category_configurations", categories);
	}

	public static CategoryLayout getByName(String serverId, String name) {
		Map<String, CategoryLayout> inServer = categories.get(serverId);
		if (inServer == null)
			return null;
		return inServer.get(name);
	}

	public static CategoryLayout create(String serverId, String name) {
		ConcurrentHashMap<String, CategoryLayout> inServer = categories.get(serverId);
		if (inServer == null) {
			inServer = new ConcurrentHashMap<>();
			categories.put(serverId, inServer);
		}
		CategoryLayout cl = new CategoryLayout();
		inServer.put(name, cl);
		return cl;
	}

	public static boolean deleteByName(Guild guild, String name) {
		Map<String, CategoryLayout> inServer = categories.get(guild.getId());
		if (inServer == null)
			return false;
		CategoryLayout cl = inServer.get(name);
		if (cl == null)
			return false;
		cl.delete(guild);
		inServer.remove(name);
		return true;
	}

}
