package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.commands.OptionNotFoundException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.CommandInteraction;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;

import java.time.Instant;

public class StalkerCommandListener {
	//TODO: user rightclick commands
	public void setupCommands(CommandManager cm) {
		cm.registerCommand(Commands.slash("avatar", "sendet einen Avatar")
				.addSubcommandGroups(new SubcommandGroupData("von", "sendet den Avatar von jemandem oder etwas")
						.addSubcommands(
								new SubcommandData("benutzer", "sendet den Avatar von einem Benutzer")
										.addOption(OptionType.USER,"benutzer","der Benutzer dessen Profilbild gesendet werden soll",true),
								new SubcommandData("benutzerid", "sendet den Avatar von einem Benutzer")
										.addOption(OptionType.STRING,"benutzerid","die ID vom Benutzer dessen Profilbild gesendet werden soll",true),
								new SubcommandData("serverid", "sendet das icon von einer Gilde (einem Server)")
										.addOption(OptionType.STRING,"guildid","die ID vom Server",false)
					)
				), (SlashCommandInteractionEvent ev) -> {
					if (!"von".equals(ev.getSubcommandGroup())) {
						QueuedLog.warning("unknown subcommand group: " + ev.getSubcommandGroup());
						return;
					}
					try {
						processAvatar(ev);
					} catch (OptionNotFoundException e) {
						QueuedLog.error("didn't find option for avatar",e);
						ev.reply("Konnte Option " + e.getOption() + " nicht extrahieren").setEphemeral(true).queue();
					}
				}
		);
	}

	private void processAvatar(SlashCommandInteractionEvent ev) throws OptionNotFoundException {
		if (ev.getSubcommandName() == null) {
			QueuedLog.error("no subcommand for avatar: " + ev.getCommandPath());
			ev.reply("Fehler bei der Identifikation des Befehls").setEphemeral(true).queue();
			return;
		}
		switch (ev.getSubcommandName()) {
			case "benutzer":
				User u = KNHelpers.getOptionMapping(ev, "benutzer").getAsUser();
				sendImageEmbed(ev,"avatar of "+u.getAsTag(),u.getEffectiveAvatarUrl());
				break;
			case "benutzerid":
				try {
					ev.getJDA().retrieveUserById(KNHelpers.getOptionMapping(ev, "benutzerid").getAsString()).queue(
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
			case "serverid":
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
				sendImageEmbed(ev, "icon of "+g.getName(), g.getIconUrl());
				break;
			default:
				QueuedLog.error("avatar subcommand not recognized: " + ev.getSubcommandName());
				ev.reply("avatar Befehl nicht korrekt erkannt").setEphemeral(true).queue();
		}
	}

	private void sendImageEmbed(CommandInteraction i, String title, String url) {
		EmbedBuilder avatarEmbed = new EmbedBuilder()
				.setTitle(title)
				.setTimestamp(Instant.now())
				.setImage(url);
		i.replyEmbeds(avatarEmbed.build()).queue();
	}
}
