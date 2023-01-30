package com.theFox6.kleinerNerd.data;

public class WrongChannelTypeException extends Exception {
    public WrongChannelTypeException(String msg) {
        super(msg);
    }

    public WrongChannelTypeException(String msg, Throwable e) {
        super(msg, e);
    }

    public static WrongChannelTypeException notMessage(String channelName) {
        return new WrongChannelTypeException("channel " + channelName + " is not a message channel");
    }
}
