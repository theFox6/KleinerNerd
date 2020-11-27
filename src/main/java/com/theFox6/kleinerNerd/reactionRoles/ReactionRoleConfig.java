package com.theFox6.kleinerNerd.reactionRoles;

import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.theFox6.kleinerNerd.data.MessageLocation;

/**
 * a JSON Serializable, single object reaction role configuration for a message
 */
public class ReactionRoleConfig {
	private MessageLocation loc;
	private ReactionRoleMap rRoles;

	public ReactionRoleConfig(MessageLocation location, ReactionRoleMap reactionRoles) {
		loc = location;
		rRoles = reactionRoles;
	}

	public static LinkedList<ReactionRoleConfig> list(Map<MessageLocation, ReactionRoleMap> rrConfig) {
		return rrConfig.entrySet().stream().map(ReactionRoleConfig::fromEntry)
				.collect(LinkedList::new,LinkedList::add,LinkedList::addAll);
	}
	
	public static ReactionRoleConfig fromEntry(Entry<MessageLocation, ReactionRoleMap> entry) {
		return new ReactionRoleConfig(entry.getKey(), entry.getValue());
	}

	public static void intoMap(LinkedList<ReactionRoleConfig> ser,
			ConcurrentHashMap<MessageLocation, ReactionRoleMap> rrConfig) {
		if (ser == null)
			return;
		ser.stream().forEach((c) -> rrConfig.put(c.loc, c.rRoles));
	}
}