package com.theFox6.kleinerNerd.listeners;

import java.util.List;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.ModLog;
import com.theFox6.kleinerNerd.storage.GuildStorage;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class ConfigurationListener {
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
    	if (raw.startsWith(KleinerNerd.prefix + "modrole")) {
    		List<Role> roles = msg.getMentionedRoles();
    		if (roles.size() < 1) {
    			if (mrid == null) {
    				chan.sendMessage("only the server owner is recognized as moderator").queue();
    				return;
    			}
    			if (modRole == null) {
    				chan.sendMessage("the set role cannot be found in this guild").queue();
    				return;
    			}
    			chan.sendMessage("current role for moderators: " + modRole.getName()).queue();
    			return;
    		} else if (roles.size() > 1) {
    			chan.sendMessage("Currently only one role can be bot moderator.").queue();
    		}
    		GuildStorage.getSettings(guild.getId()).setModRole(roles.get(0).getId());
    		chan.sendMessage("Users with the role \"" + roles.get(0).getName() + "\" can now use moderator commands.").queue();
    	} else if (raw.startsWith(KleinerNerd.prefix + "modlog")) {
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
