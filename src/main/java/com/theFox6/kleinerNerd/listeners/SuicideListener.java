package com.theFox6.kleinerNerd.listeners;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.theFox6.kleinerNerd.storage.CounterStorage;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class SuicideListener {
	Map<String,Instant> lastSuicide = new ConcurrentHashMap<>();
	
	@SubscribeEvent
	public void onMessage(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		String raw = msg.getContentRaw();
		//TODO: total suicide count
		if (raw.equals("ich geh mich umbringen")) {
			User author = msg.getAuthor();
			String authorId = author.getId();
			Instant now = Instant.now();
			Instant last = lastSuicide.get(authorId);
			lastSuicide.put(authorId, now);
			if (last != null) {
				if (last.plus(5, ChronoUnit.MINUTES).isAfter(now)) {
					msg.getChannel().sendMessage("Momentan bist du definitiv noch tod.").queue();
					return;
				}
			}
			int count = CounterStorage.getCounter("suicides_"+authorId).incrementAndGet();
			Member member = msg.getMember();
			if (member == null) {
				//probs DM
				//TODO:send a better message perhaps
				msg.getChannel().sendMessage(author.getName()+" hat sich zum " + count + ". mal umgebracht.").queue(SuicideListener::reactWithF);
				return;
			}
			msg.getChannel().sendMessage(member.getEffectiveName()+" hat sich zum " + count + ". mal umgebracht.").queue(SuicideListener::reactWithF);
		}
	}
	
	public static void reactWithF(Message msg) {
		msg.addReaction("U+1f1eb").queue();
	}
}
