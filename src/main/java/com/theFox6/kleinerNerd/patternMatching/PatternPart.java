package com.theFox6.kleinerNerd.patternMatching;

import java.util.stream.IntStream;

public interface PatternPart {
	boolean matches(String text);
	IntStream matchingStartLengths(String t);
	String stringRepresentation();
}
