package com.theFox6.kleinerNerd.commands;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import javax.annotation.Nonnull;

public interface CommandListener {
    void setupCommands(JDA jda);

    /**
     * extracts an Option and throws an exception when it is not found
     * ensures Nonnull result
     * @param ev the SlashCommandEvent to get the Option from
     * @param optionName the name/id of the Option to get
     * @return the OptionMapping for that option
     * @throws OptionNotFoundException if the option was null
     */
    @Nonnull
    static OptionMapping getOptionMapping(SlashCommandEvent ev, String optionName) throws OptionNotFoundException {
        OptionMapping om = ev.getOption(optionName);
        if (om == null)
            throw new OptionNotFoundException(ev.getCommandPath(), optionName);
        return om;
    }
}
