package com.theFox6.kleinerNerd.echo.faults;

public class UserIdNotFoundException extends ChannelRetrievalException {
	private static final long serialVersionUID = 4491239802690224990L;
	public final String userId;

	public UserIdNotFoundException(String userId) {
		super("could not find a user with the Id " + userId);
		this.userId = userId;
	}

	public UserIdNotFoundException(String userId, Throwable e) {
		super("could not find a user with the Id " + userId, e);
		this.userId = userId;
	}

}
