package com.theFox6.kleinerNerd.listeners;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

import java.time.Instant;

public class StalkerCommandListener implements CommandListener {
	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand(
				new CommandData("avatar", "sendet den avatar von einem Benutzer")
						.addOption(OptionType.USER,"benutzer","der Benutzer dessen Profilbild gesendet werden soll",true)
		).queue();
		jda.upsertCommand(
				new CommandData("servericon", "sendet das icon von einer Gilde (einem Server)")
						.addOption(OptionType.STRING,"guild","die ID vom Server",true)
		).queue();
	}

	@SubscribeEvent
	public void onCommand(SlashCommandEvent ev) {
		if (ev.getName().equals("avatar")) {
			//User c = ev.getUser();
			User u = ev.getOption("benutzer").getAsUser();
			EmbedBuilder avatarEmbed = new EmbedBuilder()
					.setTitle("avatar of "+u.getAsTag())
					.setTimestamp(Instant.now())
					.setImage(u.getEffectiveAvatarUrl());
			ev.replyEmbeds(avatarEmbed.build()).queue();
		} else if (ev.getName().equals("servericon")) {
			Guild g = ev.getJDA().getGuildById(ev.getOption("guild").getAsString());
			if (g == null) {
				ev.reply("Konnte keinen Server mit der ID finden.").setEphemeral(true).queue();
				return;
			}
			EmbedBuilder iconEmbed = new EmbedBuilder()
					.setTitle("icon of "+g.getName())
					.setTimestamp(Instant.now())
					.setImage(g.getIconUrl());
			ev.replyEmbeds(iconEmbed.build()).queue();
		}
	}
}
