package com.theFox6.kleinerNerd.patternMatching;

import java.util.function.Consumer;

/**
 * a PatternPart that can contain/nest others
 */
public interface NestingPattern extends Consumer<PatternPart> {
}
