package com.theFox6.kleinerNerd.logging;

public enum VoiceAction {
    /**
     * when a voice action has not been tracked
     */
    UNTRACKED,
    /**
     * when a user has joined a voice channel
     */
    JOIN,
    /**
     * when a user has left a voice channel
     */
    LEAVE,
    /**
     * when a user moves from one voice channel to another
     * technically this is a leave and join combination
     */
    MOVE,
    /**
     * when a user has done multiple things that were summarized
     */
    MULTIPLE,
    /**
     * when a user does something that is not yet a separate VoiceAction
     */
    OTHER;
}
