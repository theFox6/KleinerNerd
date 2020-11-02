package com.theFox6.kleinerNerd.echo.faults;

public final class TextChannelIdInGuildNotFoundException extends ChannelRetrievalException {
	private static final long serialVersionUID = 7698717751850395553L;
	public final String channelId;
	public final String guildId;
	public final String guildName;

	public TextChannelIdInGuildNotFoundException(String channelId, String guildId, String guildName) {
		super("could not find a text channel with the Id " + channelId + " within the guild \"" + guildName + "\"");
		this.channelId = channelId;
		this.guildId = guildId;
		this.guildName = guildName;
	}

}
