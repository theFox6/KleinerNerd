package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.data.MessageChannelLocation;
import com.theFox6.kleinerNerd.logging.LoggedVoiceEvent;
import com.theFox6.kleinerNerd.logging.VoiceAction;
import com.theFox6.kleinerNerd.storage.ConfigFiles;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.function.BiConsumer;

public class VoiceLoggingListener {
	// TODO: serialization of subscribers

	private Set<MessageChannelLocation> loggingTargets = new ConcurrentSkipListSet<>();
	private Map<String, LoggedVoiceEvent> lastEvents = new LinkedHashMap<>();
	
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId()))
    		return;
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	if (!raw.startsWith(KleinerNerd.prefix + "logvoice"))
    		return;
    	List<TextChannel> targets  = msg.getMentionedChannels();
    	if (targets.isEmpty()) {
			MessageChannelLocation loc = new MessageChannelLocation(chan);
			if (loggingTargets.add(loc)) {
				if (loc.type == ChannelType.PRIVATE)
					chan.sendMessage("Ich werde dir Sprachkanal Ereignisse mitteilen.").queue();
				else
					chan.sendMessage("Ich werde hier Sprachkanal Ereignisse protokollieren.").queue();
			} else {
    			if (loc.type == ChannelType.PRIVATE)
					chan.sendMessage("Ich werde dir keine Sprachkanal Ereignisse mehr schicken.").queue();
    			else
					chan.sendMessage("Ich werde hier keine Sprachkanal Ereignisse mehr protokollieren.").queue();
				loggingTargets.remove(loc);
			}
    	} else {
			//perhaps validate channels
    		targets.forEach((tc) -> {
				MessageChannelLocation loc = new MessageChannelLocation(tc);
    			if (loggingTargets.add(loc)) {
					chan.sendMessage("Ich werde Sprachkanal Ereignisse in " + tc.getAsMention() + " protokollieren.").queue();
				} else {
					loggingTargets.remove(loc);
					chan.sendMessage("Ich werde Sprachkanal Ereignisse in " + tc.getAsMention() + " nicht mehr protokollieren.").queue();
				}
			});
    	}
	}

	@SubscribeEvent
	public void onVoiceJoin(GuildVoiceJoinEvent e) {
		Member member = e.getMember();
		VoiceChannel vc = e.getChannelJoined();
		LoggedVoiceEvent last = lastEvents.get(member.getId());
		String msg = member.getEffectiveName() + " ist vc \"" + vc.getName() + "\" beigetreten";
		if (last != null && last.wasChannel(vc)) {
			if (last.getAction() == VoiceAction.LEAVE) {
				BiConsumer<MessageChannelLocation, Runnable> check = shouldBeSentTo(e.getJDA(), e.getGuild().getId());
				last.editOrSend(e.getJDA(), loggingTargets, check, member.getEffectiveName() + " hat vc \"" + vc.getName() + "\" nur für ein paar Sekunden verlassen", msg);
				if (last.isExpired())
					lastEvents.remove(member.getId(), last);
				last.updateAction(VoiceAction.MULTIPLE);
				return;
			} else if (last.getAction() != null) {
				QueuedLog.warning("unexpected last VoiceAction before join: " + last.getAction().name());
			}
		}
		sendVcUpdate(e,VoiceAction.JOIN,e.getChannelJoined(),msg);
	}
	
	@SubscribeEvent
	public void onVoiceLeave(GuildVoiceLeaveEvent e) {
		Member member = e.getMember();
		VoiceChannel vc = e.getChannelLeft();
		LoggedVoiceEvent last = lastEvents.get(member.getId());
		String msg = e.getMember().getEffectiveName() + " hat vc \"" + e.getChannelLeft().getName() + "\" verlassen";
		if (last.wasChannel(vc)) {
			if (last.getAction() == VoiceAction.JOIN) {
				if (last.isExpired()) {
					lastEvents.remove(member.getId(), last);
					return;
				}
				BiConsumer<MessageChannelLocation, Runnable> check = shouldBeSentTo(e.getJDA(), e.getGuild().getId());
				last.editOrSend(e.getJDA(), loggingTargets, check, member.getEffectiveName() + " hat vc \"" + vc.getName() + "\" nur für ein paar Sekunden betreten", msg);
				last.updateAction(VoiceAction.MULTIPLE);
				return;
			} else if (last.getAction() != null) {
				QueuedLog.warning("unexpected last VoiceAction before leave: " + last.getAction().name());
			}
		}
		sendVcUpdate(e,VoiceAction.LEAVE,e.getChannelLeft(),msg);
	}
	
	@SubscribeEvent
	public void onVoiceMove(GuildVoiceMoveEvent e) {
		String msg = e.getMember().getEffectiveName() + " hat vc von \"" + e.getChannelLeft().getName() + "\" nach \"" + e.getChannelJoined().getName() + "\" gewechselt";
		sendVcUpdate(e,VoiceAction.MOVE,e.getChannelJoined(),msg);
	}

	private void sendVcUpdate(GenericGuildVoiceEvent ev, VoiceAction action, VoiceChannel channel, String msg) {
		LoggedVoiceEvent lev = new LoggedVoiceEvent(action, channel);
		lastEvents.put(ev.getMember().getId(), lev);
		JDA jda = ev.getJDA();
		String gid = ev.getGuild().getId();
		BiConsumer<MessageChannelLocation, Runnable> check = shouldBeSentTo(jda, gid);
		loggingTargets.forEach((loc) -> check.accept(loc, () -> lev.sendMessage(jda, loc, msg)));
	}

	//TODO: rewrite

	private BiConsumer<MessageChannelLocation, Runnable> shouldBeSentTo(JDA jda, String guildFrom) {
		return (target, thenRun) -> {
			//QueuedLog.debug("shouldBeSentTo checking " + target.toString());
			switch (target.type) {
				case TEXT:
					if (!guildFrom.equals(target.guildId))
						return;
					break;
				case PRIVATE:
					jda.retrieveUserById(target.channelId).queue((u) -> {
						if (u.getMutualGuilds().stream().map(Guild::getId).anyMatch(el -> el.equals(guildFrom))) {
							thenRun.run();
						}
					}, e -> {
						if (e instanceof ErrorResponseException) {
							if (((ErrorResponseException) e).getMeaning().equals("Unknown User")) {
								QueuedLog.error("Unknown user signed up for voice logging.\n" +
										"Unknown User ID: " + target.channelId);
								return;
							}
						}
						QueuedLog.error("Couldn't retrieve user for voice logging", e);
					});
					return;
				default:
					QueuedLog.error("unsupported channel type: " + target.type.name());
			}
			thenRun.run();
		};
	}
}
