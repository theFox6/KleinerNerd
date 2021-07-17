package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.commands.CommandListener;
import com.theFox6.kleinerNerd.commands.ModOnlyCommandListener;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

public class ConfigurationListener implements CommandListener {
	//TODO: fix localization

	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand(new CommandData("modrole","Stellt die Moderatorenrolle des Servers ein"))
				.setDefaultEnabled(false)
				.addOption(OptionType.ROLE,"rolle","die Rolle, die Verwaltungs-Befehle ausführen darf",false)
				.queue(ModOnlyCommandListener::makeOwnerOnly,
						(e) -> QueuedLog.error("couldn't set up modrole command",e)
				);
		//perhaps whitelist modrole for modlog command
		jda.upsertCommand(new CommandData("modlog", "stellt den Moderator log Kanal de Servers ein"))
				.setDefaultEnabled(false)
				.addOption(OptionType.CHANNEL, "kanal", "der Kanal in den der KleineNerd Nachrichten für Moderatoren sendet", true)
				.queue(ModOnlyCommandListener::makeOwnerOnly,
						(e) -> QueuedLog.error("couldn't set up modlog command",e)
				);
	}

	@SubscribeEvent
	public void onCommand(SlashCommandEvent ev) {
		//perhaps warn if one of the commands gets run without guild
		Guild g = ev.getGuild();
		if (g == null)
			return;
		switch (ev.getName()) {
			case "modrole":
				OptionMapping role = ev.getOption("rolle");
				if (role == null) {
					GuildStorage.setModrole(g, null);
					ev.reply("Only the server owner can now use moderator commands.").queue();
				} else {
					Role nmodrole = role.getAsRole();
					GuildStorage.setModrole(g, nmodrole);
					ev.reply("Users with the role \"" + nmodrole.getName() + "\" can now use moderator commands.").queue();
				}
				break;
			case "modlog":
				OptionMapping channel = ev.getOption("kanal");
				if (channel == null) {
					GuildStorage.setModLogChannel(g, null);
					ev.reply("Es werden keine Protokoll-Nachrichten für Moderatoren mehr gesendet.").queue();
				} else {
					try {
						ModLog.setModLogChannel(g, channel.getAsMessageChannel());
					} catch (IllegalArgumentException e) {
						QueuedLog.warning("bad modlog setting", e);
						ev.reply("konnte modlog Kanal nicht einstellen: " + e.getMessage()).setEphemeral(true).queue();
						return;
					}
					ev.reply("Protokoll-Nachrichten für Moderatoren werden ab jetzt in "
							+ channel.getAsGuildChannel().getAsMention() + " gesendet.").queue();
				}
				break;
		}
	}
}
