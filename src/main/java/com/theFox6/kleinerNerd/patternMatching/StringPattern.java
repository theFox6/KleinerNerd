package com.theFox6.kleinerNerd.patternMatching;

import java.util.stream.IntStream;

public class StringPattern implements PatternPart {
	public final String text;

	public StringPattern(String text) {
		this.text = text;
	}

	@Override
	public boolean matches(String text) {
		return text.equals(this.text);
	}

	@Override
	public IntStream matchingStartLengths(String t) {
		if (t.startsWith(text))
			return IntStream.of(text.length());
		else
			return IntStream.empty();
	}

	@Override
	public String stringRepresentation() {
		return text;
	}
}
