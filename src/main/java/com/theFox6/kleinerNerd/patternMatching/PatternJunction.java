package com.theFox6.kleinerNerd.patternMatching;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PatternJunction implements PatternPart {
	private String name;
	private List<PatternPart> possibilities;
	
	public PatternJunction(String name, PatternPart... possibilities) {
		this.name = name;
		this.possibilities = Arrays.asList(possibilities);
	}

	@Override
	public boolean matches(String text) {
		// could be run parallel
		//QueuedLog.verbose("$"+name + " checking for matches in " + text);
		return possibilities.stream().anyMatch(p -> p.matches(text));
	}

	@Override
	public IntStream matchingStartLengths(String t) {
		return possibilities.stream().flatMapToInt(p -> p.matchingStartLengths(t));
	}
	
	public String stringRepresentation() {
		return "$" + name;
	}
}
