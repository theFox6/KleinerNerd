package com.theFox6.kleinerNerd.patternMatching;

import foxLog.queued.QueuedLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PatternWrapper implements PatternPart, Supplier<PatternPart>, NestingPattern {
	/**
	 * the wrapped PatternPart
	 */
	private PatternPart contents;
	
	/**
	 * creates an empty PatternWrapper
	 */
	public PatternWrapper() {
		this(null);
	}

	/**
	 * creates a PatternWrapper wrapping a given PatternPart
	 * @param wrapped the PatternPart to be wrapped
	 */
	public PatternWrapper(PatternPart wrapped) {
		contents = wrapped;
	}

	/**
	 * @return the wrapped PatternPart
	 */
	@Override
	public PatternPart get() {
		return contents;
	}

	/**
	 * sets a new PatternPart to be wrapped
	 */
	@Override
	public void accept(PatternPart p) {
		contents = p;
	}

	/**
	 * checks whether the wrapped PatternPart matches the text
	 */
	@Override
	public boolean matches(String text) {
		return contents.matches(text);
	}

	@Override
	public IntStream matchingStartLengths(String t) {
		return contents.matchingStartLengths(t);
	}

	@Override
	public String stringRepresentation() {
		return "w" + contents.stringRepresentation();
	}

	public static ConcurrentHashMap<String,PatternPart> load(BufferedReader r) throws IOException {
		QueuedLog.verbose("loading pattern");
		ConcurrentHashMap<String,PatternPart> parts = new ConcurrentHashMap<>();
		Deque<PatternPart> nesting = new LinkedList<>();
		nesting.add(new PatternWrapper());
		String neName = null;
		String l;
		int lineNumber = 0;
		while ((l = r.readLine()) != null) {
			lineNumber++;
			//QueuedLog.debug("parsing line: " + l);
			for (int i = 0; i < l.length(); i++) {
				switch (l.charAt(i)) {
				case '#': //comment start
					QueuedLog.verbose("comment: " + l.substring(i));
					i = l.length();
					break;
				case '{': //junction start
					int nameEnd = l.substring(i).indexOf(":");
					int junctionEnd = l.substring(i).indexOf("}");
					PatternPart j;
					if (nameEnd != -1 && (junctionEnd == -1 || nameEnd < junctionEnd)) {
						j = new PatternJunction(l.substring(i + 1, i + nameEnd));
						i += nameEnd;
					} else
						j = new PatternJunction("j"+lineNumber+"_"+i);
					nest(nesting,j);
					break;
				case '}': //junction end
					//perhaps log this better
					if (!(nesting.removeLast() instanceof PatternJunction))
						QueuedLog.error("unexpected junction end in line " + lineNumber);
					break;
				case '[': //sequence start
					nest(nesting,new PatternSequence());
					break;
				case ']': //sequence end
					//perhaps log this better
					if (!(nesting.removeLast() instanceof PatternSequence))
						QueuedLog.error("unexpected sequence end in line " + lineNumber);
					break;
				case '(': //optional start
					nest(nesting, new OptionalPattern());
					break;
				case ')': //optional end
					//perhaps log this better
					if (!(nesting.removeLast() instanceof OptionalPattern))
						QueuedLog.error("unexpected optional end in line " + lineNumber);
					break;
				case ',': // delimiter
					break;
				case '"': //string
					int strStart = i + 1;
					int strLen = l.substring(strStart).indexOf('"');
					if (strLen == -1) {
						QueuedLog.error("unterminated string in line " + lineNumber);
						return null;
					}
					i = strStart + strLen;
					nest(nesting, new StringPattern(l.substring(strStart, i)));
					nesting.removeLast();
					break;
				case '$': //entity
					int nameStart = i + 1;
					int nameLen = l.substring(nameStart).indexOf(';');
					if (nameLen == -1) {
						QueuedLog.error("entity name without ; in line " + lineNumber);
						i = l.length();
						break;
					}
					i = nameStart + nameLen;
					if (neName == null) {
						neName = l.substring(nameStart,i);
						int eq = l.indexOf("=");
						if (eq == -1) {
							QueuedLog.error("PatternPart forward declaration not supported in line " + lineNumber);
							i = l.length();
							break;
						}
						if (!l.substring(i + 1, eq).chars().allMatch(Character::isWhitespace))
							QueuedLog.error("Unparsed characters between PatternPart name and equals sign in line " + lineNumber);
						i = eq;
					} else {
						PatternPart e = parts.get(l.substring(nameStart, i));
						if (e == null)
							QueuedLog.error("undeclared entity " + l.substring(nameStart - 1, i + 1) + " in line "+ lineNumber);
						nest(nesting,e);
						nesting.removeLast();
					}
					break;
				default:
					char c = l.charAt(i);
					if (!Character.isWhitespace(c)) {
						QueuedLog.error("unknown character in pattern definition in line " + lineNumber +": " + c);
						return null;
					}
				}
				if (nesting.size() == 1) {
					PatternPart w = nesting.getLast();
					if (w instanceof PatternWrapper) {
						if (neName != null && ((PatternWrapper) w).get() != null) {
							parts.put(neName,nesting.removeFirst());
							nesting.addFirst(new PatternWrapper());
							neName = null;
						}
					} else {
						QueuedLog.error("outermost element was not a PatternWrapper");
					}
				}
			}
		}
		if (nesting.size() > 1) {
			QueuedLog.error("leftover opened PatternPart types: " + nesting.stream()
				.map((p) -> p.getClass().getName()).collect(Collectors.joining(", ")));
			QueuedLog.error("parsed pattern: " + nesting.getFirst().stringRepresentation());
		} else if (nesting.size() < 1) {
			QueuedLog.error("PatternWrapper was removed while parsing pattern");
			return parts;
		}
		PatternPart w = nesting.removeFirst();
		if (w instanceof PatternWrapper) {
			PatternPart e = ((PatternWrapper) w).get();
			if (e != null)
				if (neName == null)
					QueuedLog.error("parsed pattern without name");
				else
					parts.put(neName,e);
		} else {
			QueuedLog.error("outermost element was not a PatternWrapper");
			if (neName != null)
				parts.put(neName,w);
		}
		return parts;
	}
	
	private static void nest(Deque<PatternPart> nesting, PatternPart n) {
		PatternPart l = nesting.getLast();
		if (l instanceof NestingPattern)
			((NestingPattern) l).accept(n);
		else
			QueuedLog.error("PatternPart cannot be nested inside a " + l.getClass().getName());
		nesting.addLast(n);
	}
}