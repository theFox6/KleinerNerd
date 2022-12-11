package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.commands.CommandManager;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.unions.GuildChannelUnion;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class ConfigurationListener {
	public void setupCommands(CommandManager cm) {
		cm.registerCommand(
				Commands.slash("modrole","Stellt die Moderatorenrolle des Servers ein")
						.setGuildOnly(true).setDefaultPermissions(DefaultMemberPermissions.DISABLED)
						.addOption(OptionType.ROLE,"rolle","die Rolle, die Verwaltungs-Befehle ausführen darf",false),
				this::onModRoleCommand
		);
		cm.registerCommand(
				Commands.slash("modlog", "stellt den Moderator log Kanal de Servers ein")
						.setGuildOnly(true).setDefaultPermissions(DefaultMemberPermissions.DISABLED)
						.addOption(OptionType.CHANNEL, "kanal", "der Kanal in den der KleineNerd Nachrichten für Moderatoren sendet", true),
				this::onModLogCommand
		);
	}

	public void onModRoleCommand(SlashCommandInteractionEvent ev) {
		Guild g = ev.getGuild();
		if (g == null) {
			QueuedLog.warning("Couldn't get guild for modrole command.");
			ev.reply("Konnte nicht feststellen in welchem Server der Befehl ausgeführt wurde.").setEphemeral(true).queue();
			return;
		}
		OptionMapping role = ev.getOption("rolle");
		if (role == null) {
			GuildStorage.setModrole(g, null);
			ev.reply("Nur noch der Servereigentümer kann jetzt moderator Befehle benutzen.").queue();
		} else {
			Role nmodrole = role.getAsRole();
			GuildStorage.setModrole(g, nmodrole);
			ev.reply("Benutzer mit der Rolle \"" + nmodrole.getName() + "\" dürfen jetzt moderator Befehle benutzen.").queue();
		}
	}

	public void onModLogCommand(SlashCommandInteractionEvent ev) {
		Guild g = ev.getGuild();
		if (g == null) {
			QueuedLog.warning("Couldn't get guild for modlog command.");
			ev.reply("Konnte nicht feststellen in welchem Server der Befehl ausgeführt wurde.").setEphemeral(true).queue();
			return;
		}
		OptionMapping channel = ev.getOption("kanal");
		if (channel == null) {
			GuildStorage.setModLogChannel(g, null);
			ev.reply("Es werden keine Protokoll-Nachrichten für Moderatoren mehr gesendet.").queue();
		} else {
			GuildChannelUnion targetChannel = channel.getAsChannel();
			try {
				ModLog.setModLogChannel(g, targetChannel.asGuildMessageChannel());
			} catch (IllegalArgumentException e) {
				QueuedLog.warning("bad modlog setting", e);
				ev.reply("konnte modlog Kanal nicht einstellen: " + e.getMessage()).setEphemeral(true).queue();
				return;
			}
			ev.reply("Protokoll-Nachrichten für Moderatoren werden ab jetzt in "
					+ targetChannel.getAsMention() + " gesendet.").queue();
		}
	}
}
