package com.theFox6.kleinerNerd.patternMatching;

import java.util.stream.IntStream;

public class OptionalPattern implements PatternPart {
	private PatternPart optionalPart;

	public OptionalPattern(PatternPart optional) {
		optionalPart = optional;
	}

	@Override
	public boolean matches(String text) {
		return text.isEmpty() || optionalPart.matches(text);
	}

	@Override
	public IntStream matchingStartLengths(String t) {
		return IntStream.concat(optionalPart.matchingStartLengths(t),IntStream.of(0));
	}

	@Override
	public String stringRepresentation() {
		//TODO
		return "<optional>";
	}
}
