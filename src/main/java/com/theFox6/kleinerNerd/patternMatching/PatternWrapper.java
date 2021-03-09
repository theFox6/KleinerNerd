package com.theFox6.kleinerNerd.patternMatching;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import foxLog.queued.QueuedLog;

public class PatternWrapper implements PatternPart, Supplier<PatternPart>, Consumer<PatternPart> {
	/**
	 * the wrapped PatternPart
	 */
	private PatternPart contents = null;
	
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
	
	//FIXME: multiline support
	
	public static ConcurrentHashMap<String,PatternPart> load(BufferedReader r) throws IOException {
		ConcurrentHashMap<String,PatternPart> parts = new ConcurrentHashMap<>();
		QueuedLog.verbose("loading pattern");
		//file format: $laterne; = ["Ich ",{gehen: "gehe ","geh "},"mit meiner Laterne",(".")]
		String l;
		while ((l = r.readLine()) != null) {
			if (l.startsWith("#")) {
				QueuedLog.verbose(l);
			} else if (l.startsWith("$")) {
				int nameEnd = l.indexOf(";");
				if (nameEnd == -1) {
					QueuedLog.error("PatternPart name doesn't end with ;");
					continue;
				}
				String name = l.substring(1,nameEnd);
				int eq = l.indexOf("=");
				if (eq == -1) {
					QueuedLog.error("PatternPart forward declaration not supported");
					continue;
				}
			 	if (!l.substring(nameEnd + 1, eq).chars().allMatch((c) -> Character.isWhitespace(c)))
					QueuedLog.error("Unparsed characters between PatternPart name and equals sign.");
				String content = l.substring(eq+1);
				parts.put(name, parsePattern(content,parts));
			}
		}
		return parts;
	}
	
	public static PatternPart parsePattern(String pattern, ConcurrentHashMap<String, PatternPart> entities) {
		Deque<PatternPart> nesting = new LinkedList<>();
		nesting.add(new PatternWrapper());
		
		for (int i = 0; i < pattern.length(); i++) {
			char c = pattern.charAt(i);
			switch (c) {
			case '#': //comment start
				//i = l.length();
				break;
			case '{': //junction start
				int nameEnd = pattern.substring(i).indexOf(":");
				int junctionEnd = pattern.substring(i).indexOf("}");
				PatternPart j;
				if (nameEnd < junctionEnd) {
					j = new PatternJunction(pattern.substring(i + 1, nameEnd));
					i = nameEnd;
				} else
					j = new PatternJunction("j"+i);
				nest(nesting,j);
				break;
			case '}': //junction end
				//perhaps log this better
				if (!(nesting.removeLast() instanceof PatternJunction))
					QueuedLog.error("unexpected junction end");
				break;
			case '[': //sequence start
				nest(nesting,new PatternSequence());
				break;
			case ']': //sequence end
				//perhaps log this better
				if (!(nesting.removeLast() instanceof PatternSequence))
					QueuedLog.error("unexpected sequence end");
				break;
			case '(': //optional start
				nest(nesting,new OptionalPattern());
				break;
			case ')': //optional end
				//perhaps log this better
				if (!(nesting.removeLast() instanceof OptionalPattern))
					QueuedLog.error("unexpected optional end");
				break;
			case ',': // delimiter
				break;
			case '"': //string
				int strStart = i + 1;
				int strLen = pattern.substring(strStart).indexOf('"');
				if (strLen == -1) {
					QueuedLog.error("unterminated string");
					return null;
				}				
				i = strStart + strLen;
				nest(nesting, new StringPattern(pattern.substring(strStart, i)));
				break;
			case '$': //entity
				int nameStart = i + 1;
				int nameLen = pattern.substring(nameStart).indexOf(c);
				if (nameLen == -1) {
					QueuedLog.error("entity name without ;");
					return null;
				}
				i = nameStart + nameLen;
				PatternPart e = entities.get(pattern.substring(nameStart, i));
				if (e == null)
					QueuedLog.error("undeclared entity " + pattern.substring(nameStart - 1, i + 1));
				nest(nesting,e);
				nesting.removeLast();
				break;
			default:
				if (!Character.isWhitespace(c)) {
					QueuedLog.error("unknown character in pattern definition: " + c);
					return null;
				}
			}
		}
		if (nesting.size() > 1) {
			QueuedLog.error("leftover opened PatternPart types: " + nesting.stream()
				.map((p) -> p.getClass().getName()).collect(Collectors.joining(", ")));
			QueuedLog.error("parsed pattern: " + nesting.getFirst().stringRepresentation());
		} else if (nesting.size() < 1) {
			QueuedLog.error("PatternWrapper was removed while parsing pattern");
			return null;
		}
		PatternPart ret = nesting.getFirst();
		if (ret instanceof PatternWrapper)
			return ((PatternWrapper) nesting.getFirst()).get();
		else {
			QueuedLog.error("first parsing element was not a PatternWrapper");
			return ret;
		}
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