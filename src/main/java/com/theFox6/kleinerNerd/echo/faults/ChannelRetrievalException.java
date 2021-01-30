package com.theFox6.kleinerNerd.echo.faults;

public class ChannelRetrievalException extends EchoException {
	private static final long serialVersionUID = 4195290252602003960L;

	public ChannelRetrievalException(String msg) {
		super(msg);
	}

	public ChannelRetrievalException(String msg, Throwable e) {
		super(msg,e);
	}
}
