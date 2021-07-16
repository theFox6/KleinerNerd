package com.theFox6.kleinerNerd.reactionRoles;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ReactionRoleMap {
	//Reaction, Role
	@JsonProperty
	private Map<String,String> reactions;
	
	public ReactionRoleMap() {
		reactions = new ConcurrentHashMap<>();
	}
	
	public String getRoleIdForReaction(String reaction) {
		return reactions.get(reaction);
	}
	
	public void addReactionRole(String reaction, String roleid) {
		reactions.put(reaction, roleid);
	}
	
	public void removeReactionRole(String reaction) {
		reactions.remove(reaction);
	}

	public String dump() {
		return reactions.entrySet().stream().map((e) -> e.getKey() + ": " + e.getValue()).collect(Collectors.joining("\n"));
	}

	@JsonIgnore
    public boolean isEmpty() {
		return reactions.isEmpty();
    }
}
