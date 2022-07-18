package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.data.ResourceNotFoundException;
import com.theFox6.kleinerNerd.patternMatching.PatternPart;
import com.theFox6.kleinerNerd.patternMatching.PatternWrapper;
import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.storage.CounterStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

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
	private static final Duration DEFAULT_PENALTY = Duration.ofMinutes(5);

	private final Map<String, Instant> lastSuicide = new ConcurrentHashMap<>();
	private final Map<String, Duration> suicidePenalty = new ConcurrentHashMap<>();
	private final PatternPart kms;
	private final PatternPart howMany;
	
	public SuicideListener() throws IOException {
		Map<String, PatternPart> loaded;
		try (InputStream scs = SuicideListener.class.getResourceAsStream("/suicidePatterns")) {
			if (scs == null)
				throw new ResourceNotFoundException("/suicidePatterns");
			try (BufferedReader br = new BufferedReader(new InputStreamReader(scs))) {
				loaded = PatternWrapper.load(br);
			}
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
			QueuedLog.error("missing kms in suicidePatterns file");
			kms = emptyPattern;
		}
		if (loaded.containsKey("howMany"))
			howMany = loaded.get("howMany");
		else {
			QueuedLog.error("missing howMany in suicidePatterns file");
			howMany = emptyPattern;
		}
		kmsSmokeTests();
	}
	
	private void kmsSmokeTests() {
		if (!kms.matches("ich hab mit meinem leben abgeschlossen")) {
			QueuedLog.error("\"abgeschlossen\" kms does not match");
		}
		if (!kms.matches("i am going to commit suicide.")) {
			QueuedLog.error("commit suicide kms does not match");
		}
	}

	@SubscribeEvent
	public void onMessage(MessageReceivedEvent event) {
		Message msg = event.getMessage();
		MessageChannel chan = msg.getChannel();
		if (!ConfigFiles.getSuicideChannels().contains(chan.getId()))
			return;
		checkMessage(msg, chan);
	}

	@SubscribeEvent
	public void onEdit(MessageUpdateEvent event) {
		Message msg = event.getMessage();
		if (!ConfigFiles.getSuicideChannels().contains(msg.getChannel().getId()))
			return;
		MessageChannel chan = msg.getChannel();
		if (!msg.getId().equals(chan.getLatestMessageId()))
			return;
		checkMessage(msg, chan);
	}

	private void checkMessage(Message msg, MessageChannel chan) {
		String raw = msg.getContentRaw();
		String lowerRaw = raw.toLowerCase();
		if (raw.equals(KleinerNerd.prefix + "totaldeaths")) {
			chan.sendMessage("Insgesamt wurden " + CounterStorage.getUserTotalCount("suicides") + " Selbstmorde verzeichnet.").queue();
		} else if (kms.matches(lowerRaw)) {
			User author = msg.getAuthor();
			String authorId = author.getId();
			Instant last = lastSuicide.get(authorId);
			Duration penalty = suicidePenalty.getOrDefault(authorId, DEFAULT_PENALTY);
			Instant now = Instant.now();
			if (last != null) {
				Instant revive = last.plus(penalty);
				if (revive.isAfter(now)) {
					chan.sendMessage("Momentan bist du definitiv noch tot.").queue();
					return;
				}
				revive = revive.plus(penalty);
				if (revive.isAfter(now)) {
					suicidePenalty.put(authorId, DEFAULT_PENALTY.plus(Duration.between(now, revive)));
				}
			}
			lastSuicide.put(authorId, Instant.now());
			int count = CounterStorage.getUserCounter("suicides",authorId).incrementAndGet();
			Member member = msg.getMember();
			if (member == null) {
				//TODO: send a better message perhaps
				chan.sendMessage(author.getName()+" hat sich zum " + count + ". mal umgebracht.").queue(SuicideListener::reactWithF);
				return;
			}
			chan.sendMessage(member.getEffectiveName()+" hat sich zum " + count + ". mal umgebracht.").queue(SuicideListener::reactWithF);
		} else if (howMany.matches(lowerRaw)) {
			String authorId = msg.getAuthor().getId();
			int count = CounterStorage.getUserCounter("suicides",authorId).get();
			chan.sendMessage("Du hast dich " + count + " mal umgebracht.").queue();
		}
	}
	
	public static void reactWithF(Message msg) {
		msg.addReaction(Emoji.fromUnicode("U+1f1eb")).queue();
	}
	
	/* coming soon
	 * TODO: do not record your own F reacts
	 * TODO: record F reacts to suicide count messages
	 *   (not to messages in general, also the author doesn't get the achievement here)


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
		// should be added before starting to count
	}
	*/
}
