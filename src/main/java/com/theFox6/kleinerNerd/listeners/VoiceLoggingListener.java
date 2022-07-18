package com.theFox6.kleinerNerd.listeners;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.storage.ConfigFiles;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class VoiceLoggingListener {
	// TODO: serialization of subscribers
	// TODO: localization
	
	private List<String> stalkers = new LinkedList<>();
	
	//guild, channel
	private Map<String,String> guildChannels = new LinkedHashMap<>();
	
	// TODO: edit message on extremely short visits
	
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
		//TODO: respond to the interaction
    	Message msg = event.getMessage();
    	if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId()))
    		return;
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	if (!raw.startsWith(KleinerNerd.prefix + "logvoice"))
    		return;
    	List<GuildChannel> targets  = msg.getMentions().getChannels();
    	if (targets.isEmpty()) {
    		switch (chan.getType()) {
	    	case TEXT:
	    		String guildId = msg.getGuild().getId();
	    		String chanId = chan.getId();
	    		if (guildChannels.containsKey(guildId) && guildChannels.get(guildId).equals(chanId)) {
	    			guildChannels.remove(guildId, chanId);
	    			chan.sendMessage("I'll no longer send voice channel updates here").queue();
	    		} else {
	    			guildChannels.put(guildId, chanId);
	    			chan.sendMessage("I'll send voice channel updates here").queue();
	    		}
	    		//TODO: more descriptive message
	    		return;
	    	case PRIVATE:
	    		String authorId = msg.getAuthor().getId();
	    		if (stalkers.contains(authorId)) {
	    			stalkers.remove(authorId);
	    			chan.sendMessage("I'll no longer send you voice channel updates").queue();
	    		} else {
	    			stalkers.add(authorId);
	    			chan.sendMessage("I'll send you voice channel updates").queue();
	    		}
	    		return;
	    	default:
	    		QueuedLog.warning("message from unknown channel type: " + chan.getType());
	    		//return;
	    	}
    	} else {
    		targets.forEach((tc) -> {
    			guildChannels.put(tc.getGuild().getId(), tc.getId());
				TextChannel c = tc.getGuild().getTextChannelById(tc.getId());
				if (c == null)
					chan.sendMessage("Couldn't identify TextChannel " + tc.getName()).queue();
				else
					c.sendMessage("I'll send voice channel updates here")
						.queue((s) -> {}, (e) -> chan.sendMessage("An error occured when sending a message to " +
								tc.getName() + ": " + e.getMessage()).queue());
    		});
    		chan.sendMessage("Added channels for logging").queue();
    		//TODO: deactivation
    	}
	}
	
	@SubscribeEvent
	public void onVoiceJoin(GuildVoiceJoinEvent e) {
		String msg = e.getMember().getEffectiveName() + " joined vc \"" + e.getChannelJoined().getName() + '"';
		sendVcUpdate(e,msg);
	}
	
	@SubscribeEvent
	public void onVoiceLeave(GuildVoiceLeaveEvent e) {
		String msg = e.getMember().getEffectiveName() + " left vc \"" + e.getChannelLeft().getName() + '"';
		sendVcUpdate(e,msg);
	}
	
	@SubscribeEvent
	public void onVoiceMove(GuildVoiceMoveEvent e) {
		String msg = e.getMember().getEffectiveName() + " moved vc from \"" + e.getChannelLeft().getName() + "\" to \"" + e.getChannelJoined().getName() + '"';
		sendVcUpdate(e,msg);
	}

	private void sendVcUpdate(GenericGuildVoiceEvent e, String msg) {
		Guild guild = e.getGuild();
		String gId = guild.getId();
		JDA jda = e.getJDA();
		if (guildChannels.containsKey(gId)) {
			TextChannel target = jda.getTextChannelById(guildChannels.get(gId));
			if (target != null)
				target.sendMessage(msg).queue();
		}
		sendVcUpdateS(jda,msg,guild.getName());
	}

	private void sendVcUpdateS(JDA jda, String msg, String guildName) {
		stalkers.forEach((id) -> jda.openPrivateChannelById(id).queue((pc) -> pc.sendMessage(msg+ " in \"" + guildName + '"').queue()));
	}
}
