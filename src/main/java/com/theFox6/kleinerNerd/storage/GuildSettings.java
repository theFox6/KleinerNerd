package com.theFox6.kleinerNerd.storage;

public class GuildSettings {
	/**
	 * the Id of a channel where log messages will be sent for moderators
	 */
	private String modLogChannel;
	
	/**
	 * the Id of a role that is immune to spam rate limiting
	 */
	private String allowSpamRole;

	/**
	 * the Id of a role that can use moderator commands
	 */
	private String moderatorRole;

	public String getModLogChannel() {
		return modLogChannel;
	}

	public void setModLogChannel(String id) {
		modLogChannel = id;
	}
	
	public String getSpamRole() {
		return allowSpamRole;
	}

	public void setSpamRole(String id) {
		allowSpamRole = id;
	}

	public String getModRole() {
		return moderatorRole;
	}

	public void setModRole(String id) {
		moderatorRole = id;
	}
}
