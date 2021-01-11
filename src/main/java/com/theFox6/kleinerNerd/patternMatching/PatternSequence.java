package com.theFox6.kleinerNerd.patternMatching;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class PatternSequence implements PatternPart {
	//private String name;
	private List<PatternPart> parts;
	
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
}
