package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

public class ConfigurationListener implements CommandListener {
	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand(new CommandData("modrole","Stellt die Moderatorenrolle des Servers ein"))
				.setDefaultEnabled(false)
				.addOption(OptionType.ROLE,"rolle","die Rolle, die Verwaltungs-Befehle ausführen darf",false)
				.queue(
						(c) -> jda.getGuilds().forEach((g) -> c.updatePrivileges(g, CommandPrivilege.enableUser(g.getOwnerId())).queue()),
						(e) -> QueuedLog.error("couldn't set up modrole command",e)
				);
		//TODO: Whitelist modrole
		jda.upsertCommand(new CommandData("modlog", "stellt den Moderator log Kanal de Servers ein"))
				.setDefaultEnabled(false)
				.addOption(OptionType.CHANNEL, "kanal", "der Kanal in den der KleineNerd Nachrichten für Moderatoren sendet", true)
				.queue(
						(c) -> jda.getGuilds().forEach((g) -> c.updatePrivileges(g, CommandPrivilege.enableUser(g.getOwnerId())).queue()),
						(e) -> QueuedLog.error("couldn't set up modrole command",e)
				);
	}

	@SubscribeEvent
	public void onCommand(SlashCommandEvent ev) {
		switch (ev.getName()) {
			case "modrole":
				OptionMapping role = ev.getOption("rolle");
				if (role == null) {
					GuildStorage.getSettings(ev.getGuild().getId()).setModRole(null);
				} else {
					Role nmodrole = role.getAsRole();
					GuildStorage.getSettings(ev.getGuild().getId()).setModRole(nmodrole.getId());
					ev.reply("Users with the role \"" + nmodrole.getName() + "\" can now use moderator commands.").queue();
				}
				break;
			case "modlog":
				OptionMapping channel = ev.getOption("kanal");
				if (channel == null) {
					ModLog.setModLogChannel(ev.getGuild(), null);
					ev.reply("modlog Kanal zurückgesetzt").queue();
				} else {
					try {
						ModLog.setModLogChannel(ev.getGuild(), channel.getAsMessageChannel());
					} catch (IllegalArgumentException e) {
						QueuedLog.warning("bad modlog setting", e);
						ev.reply("konnte modlog Kanal nicht einstellen: " + e.getMessage()).setEphemeral(true).queue();
						return;
					}
					ev.reply("\"" + channel.getAsGuildChannel().getName() + "\" als modlog Kanal eingestellt.").queue();
				}
				break;
		}
	}
}
