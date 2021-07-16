package com.theFox6.kleinerNerd.listeners;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.time.Instant;

public class StalkerCommandListener implements CommandListener {
	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand(new CommandData("avatar", "sendet einen Avatar")
				.addSubcommands(
						new SubcommandData("von-benutzer", "sendet den Avatar von einem Benutzer")
								.addOption(OptionType.USER,"benutzer","der Benutzer dessen Profilbild gesendet werden soll",true),
						new SubcommandData("von-benutzerid", "sendet den Avatar von einem Benutzer")
								.addOption(OptionType.STRING,"benutzerid","die ID vom Benutzer dessen Profilbild gesendet werden soll",true),
						new SubcommandData("von-serverid", "sendet das icon von einer Gilde (einem Server)")
								.addOption(OptionType.STRING,"guildid","die ID vom Server",false)
				)
		).queue();
	}

	@SubscribeEvent
	public void onCommand(SlashCommandEvent ev) {
		if (ev.getName().equals("avatar")) {
			switch (ev.getSubcommandName()) {
				case "von-benutzer":
					User u = ev.getOption("benutzer").getAsUser();
					sendImageEmbed(ev,"avatar of "+u.getAsTag(),u.getEffectiveAvatarUrl());
					break;
				case "von-benutzerid":
					try {
						ev.getJDA().retrieveUserById(ev.getOption("benutzerid").getAsString()).queue(
								(ru) -> sendImageEmbed(ev, "avatar of " + ru.getAsTag(), ru.getEffectiveAvatarUrl()),
								(e) -> {
									ev.reply("Konnte Benutzer mit angegebener ID nicht finden.").setEphemeral(true).queue();
									if (e.getMessage().equals("10013: Unknown User"))
										QueuedLog.debug("couldn't find the user with the given id");
									else
										QueuedLog.warning("couldn't retrieve user", e);
								}
						);
					} catch (NumberFormatException e) {
						QueuedLog.debug("user didn't enter numeric ID: "+e.getMessage());
						ev.reply(e.getMessage()).setEphemeral(true).queue();
					}
					break;
				case "von-serverid":
					OptionMapping gid = ev.getOption("guildid");
					Guild g;
					if (gid == null) {
						g = ev.getGuild();
						if (g == null) {
							ev.reply("Keine Server ID gegeben und nicht in einem Server gesendet.")
									.setEphemeral(true).queue();
							return;
						}
					} else {
						g = ev.getJDA().getGuildById(gid.getAsString());
						if (g == null) {
							ev.reply("Konnte keinen Server mit der ID finden.").setEphemeral(true).queue();
							return;
						}
					}

					EmbedBuilder iconEmbed = new EmbedBuilder()
							.setTitle("icon of "+g.getName())
							.setTimestamp(Instant.now())
							.setImage(g.getIconUrl());
					ev.replyEmbeds(iconEmbed.build()).queue();
					break;
				default:
					QueuedLog.warning("avatar subcommand not recognized");
			}
		}
	}

	private void sendImageEmbed(Interaction i, String title, String url) {
		EmbedBuilder avatarEmbed = new EmbedBuilder()
				.setTitle(title)
				.setTimestamp(Instant.now())
				.setImage(url);
		i.replyEmbeds(avatarEmbed.build()).queue();
	}
}
