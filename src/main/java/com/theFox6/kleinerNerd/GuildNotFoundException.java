package com.theFox6.kleinerNerd;

public class GuildNotFoundException extends Exception {
	private static final long serialVersionUID = -656581912050120078L;

	public GuildNotFoundException(String msg) {
		super(msg);
	}

	public GuildNotFoundException(String msg, Throwable e) {
		super(msg, e);
	}

	public static GuildNotFoundException forGuildId(String guildId) {
		return new GuildNotFoundException("could not find a guild with the Id " + guildId);
	}

}
