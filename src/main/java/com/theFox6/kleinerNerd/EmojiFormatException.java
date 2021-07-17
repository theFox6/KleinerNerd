package com.theFox6.kleinerNerd;

public class EmojiFormatException extends Exception {
    public EmojiFormatException(String message, String emojiRaw) {
        super(message + " for emoji \"" + emojiRaw + "\"");
    }
}
