package com.theFox6.kleinerNerd.listeners;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class HelpListener implements CommandListener {
	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand(
				new CommandData("help","Zeigt Hilfe zu bestimmten themen, allerdings keine allgemeine Hilfe.")
				.addOption(OptionType.STRING, "thema", "Thema, zu dem Hilfe gezeigt werden soll.", false)
		).queue();
	}

	@SubscribeEvent
	public void onCommand(SlashCommandEvent event) {
		if (event.getName().equals("help")) {
			OptionMapping topicOption = event.getOption("thema");
			if (topicOption == null) {
				//perhaps add general help
				event.reply("Help!").queue();
				return;
			}
			String topic = topicOption.getAsString();
			if (topic.equals("location format")) {
				event.reply("Locations should be provided in the format:\n" +
						"`PRIVATE userID` or `TEXT guildID channelID`").queue();
			} else {
				event.reply("Got no help for: " + topic).queue(
						(m) -> event.getChannel().sendMessage("Sorry.").queue());
			}
		}
	}
}
