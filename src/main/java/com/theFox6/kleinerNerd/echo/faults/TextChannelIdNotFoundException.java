package com.theFox6.kleinerNerd.echo.faults;

import com.theFox6.kleinerNerd.ChannelRetrievalException;

public final class TextChannelIdNotFoundException extends ChannelRetrievalException {
	private static final long serialVersionUID = 8990027446031725320L;
	public final String channelId;

	public TextChannelIdNotFoundException(String channelId) {
		super("could not find a text channel with the Id " + channelId);
		this.channelId = channelId;
	}
}
