package com.theFox6.kleinerNerd.commands;

public class IllegalOptionException extends Exception {
    private final String commandPath;
    protected final String optionName;

    public IllegalOptionException(String commandPath, String option, String message) {
        super(message);
        this.commandPath = commandPath;
        optionName = option;
    }

    public String getOption() {
        return optionName;
    }

    public String getCommandPath() {
        return commandPath;
    }
}
