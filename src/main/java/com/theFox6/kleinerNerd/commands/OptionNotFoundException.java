package com.theFox6.kleinerNerd.commands;

public class OptionNotFoundException extends IllegalOptionException {
    public OptionNotFoundException(String commandPath, String option) {
        super(commandPath, option, "could not find \""+option+"\" option for command \"" + commandPath + "\"");
    }
}
