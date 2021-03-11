package com.theFox6.kleinerNerd.patternMatching;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class PatternJunction implements PatternPart, NestingPattern {
	private String name;
	private Collection<PatternPart> possibilities;
	
	/**
	 * creates a new empty but mutable PatternJunction
	 * 
	 * @param name the name of the PatternJunction (will be displayed in the stringRepresentation)
	 */
	public PatternJunction(String name) {
		this.name = name;
		this.possibilities = new ConcurrentLinkedQueue<>();
	}

	/**
	 * creates a new PatternJunction with the given possibilities
	 * this makes the possibility list have a fixed size
	 * since it is using {@link java.util.Arrays.asList}
	 * 
	 * @param name the name of the PatternJunction (will be displayed in the stringRepresentation)
	 * @param possibilities the PatternParts of which any has to match
	 */
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

	/**
	 * add another option to the junction
	 * @param possibility the pattern part that should be one of the options in this place
	 */
	@Override
	public void accept(PatternPart possibility) {
		possibilities.add(possibility);
	}
}
