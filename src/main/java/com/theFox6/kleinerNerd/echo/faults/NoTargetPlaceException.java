package com.theFox6.kleinerNerd.echo.faults;

public class NoTargetPlaceException extends InvalidTargetPlaceException {
	private static final long serialVersionUID = -2794635003754642326L;

	public NoTargetPlaceException() {
		super("no target place");
	}

	public NoTargetPlaceException(String msg) {
		super(msg);
	}
}
