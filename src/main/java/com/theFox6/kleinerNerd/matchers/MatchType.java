package com.theFox6.kleinerNerd.matchers;

public enum MatchType {
	/**
	 * a command/trigger/argument Pattern could be fully matched
	 */
	FULL_MATCH,
	
	/**
	 * a command/trigger/argument Pattern was recognized but missing optional parameters
	 */
	MISSING_OPTIONALS,
	
	/**
	 * a command/trigger/argument Pattern was recognized but missing mandatory parameters
	 */
	MISSING_PARAMS,
	
	/**
	 * a command/trigger/argument Pattern could not found in the given sequence
	 */
	NO_MATCH
}
