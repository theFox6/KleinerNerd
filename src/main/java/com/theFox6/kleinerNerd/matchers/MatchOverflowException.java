package com.theFox6.kleinerNerd.matchers;

/**
 * indicates that there are more matches then expected
 */
public class MatchOverflowException extends RuntimeException {
	private static final long serialVersionUID = -4171036292426641811L;

	public MatchOverflowException(String message) {
		super(message);
	}
}
