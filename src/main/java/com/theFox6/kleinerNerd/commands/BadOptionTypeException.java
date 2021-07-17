package com.theFox6.kleinerNerd.commands;

import net.dv8tion.jda.api.interactions.commands.OptionType;

public class BadOptionTypeException extends IllegalOptionException {
    public BadOptionTypeException(String optionName, String commandPath, OptionType actualOptionType, OptionType expectedOptionType, Class<?> optionClass) {
        super(commandPath, optionName, "bad option type in command \"" + commandPath + "\" for option \""+optionName+"\" expected " + expectedOptionType.name() + " convertable to " + optionClass.getName() + " got " + actualOptionType.name());
    }
}
