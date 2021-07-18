package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Random;

public class KNHelpers {
	public static Random rng = new Random(System.nanoTime()^System.currentTimeMillis());
	
	public static <T> T randomElement(List<T> l) {
		return l.get(rng.nextInt(l.size()));
	}

	public static <T> T randomElement(T[] a) {
		return a[rng.nextInt(a.length)];
	}

    /**
     * extracts an Option and throws an exception when it is not found
     * ensures Nonnull result
     * @param ev the SlashCommandEvent to get the Option from
     * @param optionName the name/id of the Option to get
     * @return the OptionMapping for that option
     * @throws OptionNotFoundException if the option was null
     */
    @Nonnull
    public static OptionMapping getOptionMapping(SlashCommandEvent ev, String optionName) throws OptionNotFoundException {
        OptionMapping om = ev.getOption(optionName);
        if (om == null)
            throw new OptionNotFoundException(ev.getCommandPath(), optionName);
        return om;
    }
}
