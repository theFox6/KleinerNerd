package com.theFox6.kleinerNerd.roles;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.storage.StorageManager;
import foxLog.queued.QueuedLog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class GetRoleStorage {
    private static final Path rsf = KleinerNerd.dataFolder.resolve("getrole.json");
    private static Map<String, Map<String, Set<String>>> rolesByGuildAndCategory;

    public static void load() {
        if (Files.exists(rsf)) {
            TypeFactory tf = TypeFactory.defaultInstance();
            JavaType s = tf.constructType(String.class);
            JavaType storedType = tf.constructMapType(ConcurrentHashMap.class, s,
                    tf.constructMapType(ConcurrentHashMap.class, s, tf.constructCollectionType(ConcurrentSkipListSet.class, s)));
            try {
                rolesByGuildAndCategory = new ObjectMapper().readValue(Files.newBufferedReader(rsf), storedType);
            } catch (JsonParseException e) {
                QueuedLog.error("parse error while trying to load roles settings", e);
                StorageManager.loadingError("getrole","JSON parse error while loading roles settings");
            } catch (JsonMappingException e) {
                QueuedLog.error("mapping error while trying to load roles settings", e);
                StorageManager.loadingError("getrole","JSON mapping error while loading roles settings");
            } catch (IOException e) {
                QueuedLog.error("io error while trying to load roles settings", e);
                StorageManager.loadingError("getrole","io error while loading roles settings");
            }
        } else {
            rolesByGuildAndCategory = new ConcurrentHashMap<>();
        }
    }

    public static void save() {
        StorageManager.save("getrole", rolesByGuildAndCategory);
    }

    public static Map<String,Set<String>> getRolesByCategory(String guildId) {
        if (!rolesByGuildAndCategory.containsKey(guildId))
            return null;
        return rolesByGuildAndCategory.get(guildId).entrySet().stream()
                .collect(ConcurrentHashMap::new,
                        (m,e) -> m.put(e.getKey(),Collections.unmodifiableSet(e.getValue())),
                        Map::putAll);
    }

    public static void addRole(String guildid, String category, String roleid) {
        rolesByGuildAndCategory
                .computeIfAbsent(guildid, (k) -> new ConcurrentHashMap<>())
                .computeIfAbsent(category, (k) -> ConcurrentHashMap.newKeySet())
                .add(roleid);
    }

    public static boolean removeRole(String guildid, String category, String roleid) {
        if (!rolesByGuildAndCategory.containsKey(guildid))
            return false;
        if (!rolesByGuildAndCategory.get(guildid).containsKey(category))
            return false;
        return rolesByGuildAndCategory.get(guildid).get(category).remove(roleid);
    }
}
