package com.theFox6.kleinerNerd;

public class ChannelRetrievalException extends Exception {
	private static final long serialVersionUID = 4195290252602003960L;

	public ChannelRetrievalException(String msg) {
		super(msg);
	}

	public ChannelRetrievalException(String msg, Throwable e) {
		super(msg, e);
	}

	public static ChannelRetrievalException forChannelId(String channelId) {
		return new ChannelRetrievalException("could not retrieve channel with ID: " + channelId);
	}
}
