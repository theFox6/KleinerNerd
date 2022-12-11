package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.storage.ConfigFiles;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.AudioChannelUnion;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class VoiceLoggingListener {
	// TODO: serialization of subscribers
	// TODO: localization
	
	private final List<String> stalkers = new LinkedList<>();
	
	//guild, channel
	private final Map<String,String> guildChannels = new LinkedHashMap<>();
	
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
				//TODO: it may not be a text channel
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
	public void onVoiceUpdate(GuildVoiceUpdateEvent e) {
		//TODO: edit message when things change rapidly (short disconnect, short visit)
		AudioChannelUnion old = e.getChannelLeft();
		AudioChannelUnion joined = e.getChannelJoined();
		if (old == null) {
			if (joined == null)
				QueuedLog.warning("guild voice update without channel change");
			else
				onVoiceJoin(e, joined);

		} else {
			if (joined == null)
				onVoiceLeave(e, old);
			else
				onVoiceMove(e, old, joined);
		}
	}
	
	public void onVoiceJoin(GuildVoiceUpdateEvent e, AudioChannelUnion joined) {
		String msg = e.getMember().getEffectiveName() + " joined vc \"" + joined.getName() + '"';
		sendVcUpdate(e,msg);
	}
	
	public void onVoiceLeave(GuildVoiceUpdateEvent e, AudioChannelUnion left) {
		String msg = e.getMember().getEffectiveName() + " left vc \"" + left.getName() + '"';
		sendVcUpdate(e,msg);
	}
	
	public void onVoiceMove(GuildVoiceUpdateEvent e, AudioChannelUnion left, AudioChannelUnion joined) {
		String msg = e.getMember().getEffectiveName() + " moved vc from \"" + left.getName() + "\" to \"" + joined.getName() + '"';
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
