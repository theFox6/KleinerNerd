package com.theFox6.kleinerNerd.listeners;

import java.util.List;

import com.theFox6.kleinerNerd.KNHelpers;
import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.storage.GuildStorage;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

public class ConfigurationListener implements CommandListener {
	@Override
	public void setupCommands(JDA jda) {
		jda.upsertCommand(new CommandData("modrole","Stellt die Moderatorenrolle des Servers ein"))
				.setDefaultEnabled(false)
				.addOption(OptionType.ROLE,"rolle","die Rolle, die Verwaltungs-Befehle ausführen darf",false)
				.queue();
	}

	//TODO: Whitelist guild members

	@SubscribeEvent
	public void onCommand(SlashCommandEvent ev) {
		switch (ev.getName()) {
			case "modrole":
				OptionMapping role = ev.getOption("role");
				if (role == null) {
					GuildStorage.getSettings(ev.getGuild().getId()).setModRole(null);
				} else {
					Role nmodrole = role.getAsRole();
					GuildStorage.getSettings(ev.getGuild().getId()).setModRole(nmodrole.getId());
					ev.reply("Users with the role \"" + nmodrole.getName() + "\" can now use moderator commands.").queue();
				}
				break;
		}
	}

	@SubscribeEvent
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	if (msg.isWebhookMessage()) {
    		//for now WebHooks are not privileged to change things
    		return;
    	}
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	Member author = msg.getMember();
    	Guild guild = msg.getGuild();
    	boolean authorized = author.hasPermission(Permission.ADMINISTRATOR);
    	String mrid = GuildStorage.getSettings(guild.getId()).getModRole();
    	Role modRole = null;
    	if (mrid != null) {
    		modRole = guild.getRoleById(mrid);
    		if (modRole == null) {
    			QueuedLog.warning("set mod role could not be found");
    		} else {
    			if (author.getRoles().contains(modRole)) {
    				authorized = true;
    			}
    		}
    	}
    	if (!authorized)
    		return;
    	if (raw.startsWith(KleinerNerd.prefix + "modlog")) {
    		List<TextChannel> target = msg.getMentionedChannels();
    		if (target.size() < 1) {
    			chan.sendMessage("please mention a target channel").queue();
    			return;
    		} else if (target.size() > 1) {
    			chan.sendMessage("mentioned too many channels first will be used").queue();
    		}
    		try {
    			ModLog.setModLogChannel(msg.getGuild(), target.get(0));
    		} catch (IllegalArgumentException e) {
    			QueuedLog.error("bad modlog setting",e);
    			chan.sendMessage("could not set modlog channel: " + e.getMessage()).queue();
    			return;
    		}
    		chan.sendMessage("Set \"" + target.get(0).getName() + "\" as modlog channel.").queue();
    	}
	}
}
