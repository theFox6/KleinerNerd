package com.theFox6.kleinerNerd.reactionRoles;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ReactionRoleMap {
	//Reaction, Role
	private Map<String,String> reactions;
	
	public ReactionRoleMap() {
		reactions = new ConcurrentHashMap<>();
	}
	
	public String getRoleIdForReaction(String reaction) {
		return reactions.get(reaction);
	}
	
	public void addReactionRole(String reaction, String role) {
		reactions.put(reaction, role);
	}
	
	public void removeReactionRole(String reaction) {
		reactions.remove(reaction);
	}
}