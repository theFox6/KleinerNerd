package com.theFox6.kleinerNerd.listeners;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.patternMatching.PatternPart;
import com.theFox6.kleinerNerd.patternMatching.PatternWrapper;
import com.theFox6.kleinerNerd.storage.CounterStorage;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class SuicideListener {
	private static final PatternPart emptyPattern = new PatternPart() {
		@Override
		public boolean matches(String text) {
			return false;
		}

		@Override
		public IntStream matchingStartLengths(String t) {
			return IntStream.of();
		}

		@Override
		public String stringRepresentation() {
			return "<empty>";
		}
	};

	private Map<String,Instant> lastSuicide = new ConcurrentHashMap<>();
	private final PatternPart kms;
	private final PatternPart howMany;
	
	public SuicideListener() throws IOException {
		URL patternFile = KleinerNerd.class.getClassLoader().getResource("suicidePatterns");
		if (patternFile == null) {
			throw new FileNotFoundException("sucicidePatterns file not found in resources");
		}
		File file = new File(patternFile.getFile());
		if (!file.exists()) {
			throw new FileNotFoundException("sucicidePatterns file found but does not exist");
		}
		Map<String, PatternPart> loaded;
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			loaded = PatternWrapper.load(br);
		}
		if (loaded == null) {
			QueuedLog.error("suicidePatterns were not loaded");
			kms = emptyPattern;
			howMany = emptyPattern;
			return;
		}
		if (loaded.containsKey("kms"))
			kms = loaded.get("kms");
		else {
			QueuedLog.error("missing kms in sucicidePatterns file");
			kms = emptyPattern;
		}
		if (loaded.containsKey("howMany"))
			howMany = loaded.get("howMany");
		else {
			QueuedLog.error("missing howMany in sucicidePatterns file");
			howMany = emptyPattern;
		}
		kmsSmokeTests();
	}
	
	private void kmsSmokeTests() {
		if (!kms.matches("ich hab mit meinem leben abgeschlossen")) {
			QueuedLog.error("abgeschlossen kms does not match");
		}
		if (!kms.matches("i am going to commit suicide.")) {
			QueuedLog.error("commit suicide kms does not match");
		}
	}

	@SubscribeEvent
	public void onMessage(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		String raw = msg.getContentRaw();
		String lowerRaw = raw.toLowerCase();
		//TODO: total suicide count
		if (raw.equals(KleinerNerd.prefix + "totaldeaths")) {
			msg.getChannel().sendMessage(Integer.toString(CounterStorage.getUserTotalCount("suicides"))).queue();
		} else if (kms.matches(lowerRaw)) {
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
		} else if (howMany.matches(lowerRaw)) {
			String authorId = msg.getAuthor().getId();
			int count = CounterStorage.getUserCounter("suicides",authorId).get();
			msg.getChannel().sendMessage("Du hast dich " + count + " mal umgebracht.").queue();
		}
	}
	
	public static void reactWithF(Message msg) {
		msg.addReaction("U+1f1eb").queue();
	}
	
	/* coming soon
	// TODO: do not record your own F reacts

	@SubscribeEvent
	public void onReact(GuildMessageReactionAddEvent event) {
		if (event.getReactionEmote().getEmoji().equals("U+1f1eb")) {
			String userId = event.getUserId();
			checkFReactAchievement(userId, CounterStorage.getUserCounter("f_reacts", userId).incrementAndGet());
		}
	}
	
	@SubscribeEvent
	public void onReactRemove(GuildMessageReactionRemoveEvent event) {
		if (event.getReactionEmote().getEmoji().equals("U+1f1eb")) {
			CounterStorage.getUserCounter("f_reacts", event.getUserId()).getAndDecrement();
		}
	}

	private void checkFReactAchievement(String userId, int fcount) {
		// TODO achievements for 10, 20, 50, 100, 200, 1000
		// should be added before release
	}
	*/
}
