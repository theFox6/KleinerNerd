package com.theFox6.kleinerNerd.matchers;

public abstract class CommandMatch {

	public final MatchType match;
	private final String args;

	/**
	 * construct a new matching result for a command matcher
	 * @param match what the actual match result is to be treated as
	 * @param args the further textual arguments of the command or the raw match source
	 */
	public CommandMatch(MatchType match, String args) {
		this.match = match;
		this.args = args;
	}

	public String getArgs() {
		return args;
	}

	public boolean isFullMatch() {
		return match == MatchType.FULL_MATCH;
	}

	public boolean isValidMatch() {
		return match == MatchType.MISSING_OPTIONALS || isFullMatch();
	}

}
