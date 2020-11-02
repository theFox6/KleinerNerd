package com.theFox6.kleinerNerd.echo.faults;

public class EchoException extends Exception {
	private static final long serialVersionUID = -7909518752108441220L;

	public EchoException(String msg) {
		super(msg);
	}
}
