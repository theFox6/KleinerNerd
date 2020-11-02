package com.theFox6.kleinerNerd.echo.faults;

public final class GuildIdNotFoundException extends ChannelRetrievalException {
	private static final long serialVersionUID = -656581912050120078L;
	public final String guildId;

	public GuildIdNotFoundException(String guildId) {
		super("could not find a guild with the Id " + guildId);
		this.guildId = guildId;
	}

}
