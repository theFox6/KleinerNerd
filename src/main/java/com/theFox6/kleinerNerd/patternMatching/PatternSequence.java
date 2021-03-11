package com.theFox6.kleinerNerd.patternMatching;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.IntStream;

public class PatternSequence implements PatternPart, NestingPattern {
	//private String name;
	private Collection<PatternPart> parts;

	public PatternSequence() {
		this.parts = new ConcurrentLinkedQueue<>();
	}

	public PatternSequence(/*String name,*/ PatternPart... parts) {
		//this.name = name;
		this.parts = Arrays.asList(parts);
	}

	@Override
	public boolean matches(String text) {
		int tl = text.length();
		return matchingStartLengths(text).anyMatch((m) -> m == tl);
	}

	@Override
	public IntStream matchingStartLengths(String text) {
		//QueuedLog.verbose("checking whether "+text+" starts with the sequence " + parts.stream().map(PatternPart::stringRepresentation).collect(Collectors.joining()));
		IntStream lengths = IntStream.of(0);
		for (PatternPart pp : parts) {
			lengths = lengths.flatMap((l) -> pp.matchingStartLengths(text.substring(l)).map(m -> m + l));
		}
		return lengths;
	}

	@Override
	public String stringRepresentation() {
		// TODO
		return "<sequence>";
	}

	/**
	 * adds a PatternPart to the end of the sequence
	 * @param patternPart the part that should be added to the sequence
	 */
	@Override
	public void accept(PatternPart patternPart) {
		parts.add(patternPart);
	}
}
