package com.theFox6.kleinerNerd.listeners;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.patternMatching.OptionalPattern;
import com.theFox6.kleinerNerd.patternMatching.PatternJunction;
import com.theFox6.kleinerNerd.patternMatching.PatternPart;
import com.theFox6.kleinerNerd.patternMatching.PatternSequence;
import com.theFox6.kleinerNerd.patternMatching.StringPattern;
import com.theFox6.kleinerNerd.storage.CounterStorage;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class SuicideListener {
	private Map<String,Instant> lastSuicide = new ConcurrentHashMap<>();
	private final PatternPart kms;
	private static final List<String> howMany = Arrays.asList(
			"wie oft habe ich mich schon umgebracht?","wie oft habe ich mich schon umgebracht",
			"wie oft habe ich mich schon erhängt?","wie oft habe ich mich schon erhängt");
	
	public SuicideListener() {
		OptionalPattern od = new OptionalPattern(new StringPattern("."));
		PatternPart z = new PatternJunction("ziel ",
				new StringPattern("ich gehe "),
				new StringPattern("ich geh "),
				new StringPattern("ich will "),
				new StringPattern("ich möchte "));
		PatternPart haben = new PatternJunction("haben ",
				new StringPattern("ich habe "),
				new StringPattern("ich hab "));
		kms = new PatternJunction("kms",
				new PatternJunction("umbringen",
						new PatternSequence(z, new StringPattern("mich umbringen"), od),
						new PatternSequence(new StringPattern("ich bring mich um"),od),
						new PatternSequence(new StringPattern("ich bringe mich um"),od)
					),
				new PatternSequence(z, new StringPattern("erhängen"),od),
				new PatternSequence(z, new StringPattern("sterben"),od),
				new PatternSequence(z, new StringPattern("mein leben beenden"), od),
				new PatternSequence(z, new StringPattern("nicht mehr leben"),od),
				new PatternSequence(z, new StringPattern("mich ertränken"),od),
				new PatternJunction("lebenAbgeschlossen",
						new PatternSequence(haben,new StringPattern("mein leben abgeschlossen"),od),
						new PatternSequence(haben,new StringPattern("mit meinem leben abgeschlossen"),od),
						new PatternSequence(haben,new StringPattern("abgeschlossen mit meinem leben"),od)
					),
				new PatternSequence(new StringPattern("i want to die"),od)
			);
		//just a smoke test
		if (!kms.matches("ich hab mit meinem leben abgeschlossen")) {
			QueuedLog.error("kms does not match");
		}
	}
	
	@SubscribeEvent
	public void onMessage(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		String raw = msg.getContentRaw();
		//TODO: total suicide count
		if (raw.equals(KleinerNerd.prefix + "totaldeaths")) {
			CounterStorage.getUserTotalCount("suicides");
		} else if (kms.matches(raw.toLowerCase())) {
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
			int count = CounterStorage.getUserCounter("suicides",authorId).incrementAndGet();
			Member member = msg.getMember();
			if (member == null) {
				//probs DM
				//TODO:send a better message perhaps
				msg.getChannel().sendMessage(author.getName()+" hat sich zum " + count + ". mal umgebracht.").queue(SuicideListener::reactWithF);
				return;
			}
			msg.getChannel().sendMessage(member.getEffectiveName()+" hat sich zum " + count + ". mal umgebracht.").queue(SuicideListener::reactWithF);
		} else if (howMany.stream().anyMatch((t) -> t.equalsIgnoreCase(raw))) {
			String authorId = msg.getAuthor().getId();
			int count = CounterStorage.getUserCounter("suicides",authorId).get();
			msg.getChannel().sendMessage("Du hast dich " + count + " mal umgebracht.").queue();
		}
	}
	
	public static void reactWithF(Message msg) {
		msg.addReaction("U+1f1eb").queue();
	}
}
