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
		kms = loaded.get("kms");
		if (kms == null)
			QueuedLog.error("missing kms in sucicidePatterns file");
		howMany = loaded.get("howMany");
		if (howMany == null)
			QueuedLog.error("missing kms in sucicidePatterns file");
		kmsSmokeTests();
	}
	
	/*
	public SuicideListener() {
		OptionalPattern od = new OptionalPattern(new StringPattern("."));
		PatternPart z = new PatternJunction("ziel",
				new StringPattern("ich gehe "),
				new StringPattern("ich geh "),
				new StringPattern("ich will "),
				new StringPattern("ich möchte "));
		PatternPart haben = new PatternJunction("haben",
				new StringPattern("ich habe "),
				new StringPattern("ich hab "));
		PatternPart iamgoingto = new PatternJunction("iamgoingto",
				new StringPattern("ima "),
				new StringPattern("i want to"),
				new StringPattern("i wanna"),
				new PatternSequence(
						new PatternJunction("im",new StringPattern("i am "), new StringPattern("i'm "), new StringPattern("im ")),
						new PatternJunction("gonna", new StringPattern("gonna "), new StringPattern("going to "))
					)
			);
		PatternPart muffinSong = new PatternJunction("miffinSong",
				new StringPattern("who wants a muffin?"),
				new StringPattern("have you had a muffin today?"),
				new PatternSequence(
						new StringPattern("somebody kill me"),
						new StringPattern("it's muffin time"),
						new OptionalPattern(new PatternJunction("end",new StringPattern("."),new StringPattern("!")))
					)
			);
		//TODO:
		// i will commit not living
		// ich gehe in die Steckdose fassen 
		// Ich gehe mich anzünden
		// ich möchte mich beenden
		// Ich spring vorn Zug
		kms = new PatternJunction("kms",
				new PatternSequence(z, new PatternJunction("lebenbeenden",
						new StringPattern("mein leben beenden"),
						new StringPattern("meine pulsadern aufschlitzen"),
						new StringPattern("mir die pulsadern aufschlitzen"),
						new StringPattern("mir meine pulsadern aufschlitzen"),
						new StringPattern("nicht mehr leben"),
						new StringPattern("sterben"),
						new StringPattern("von einer klippe springen"),
						new StringPattern("von ner klippe springen"),
						new PatternSequence(new StringPattern("mich "), new PatternJunction("totmachen",
								new StringPattern("aufschlitzen"),
								new StringPattern("aufschlitzen"),
								new StringPattern("beenden"),
								new StringPattern("erhängen"),
								new StringPattern("ertränken"),
								new StringPattern("erschießen"),
								new StringPattern("tot machen"),
								new StringPattern("töten"),
								new StringPattern("umbringen"),
								//new StringPattern("verbuddeln"),
								//new StringPattern("vergraben"),
								new StringPattern("vergiften"),
								new StringPattern("vom bus überfahren lassen"),
								new StringPattern("von einem bus überfahren lassen"),
								new StringPattern("von einer klippe stürzen"),
								new StringPattern("von ner klippe stürzen")
						))
					), od),
				new PatternSequence(new StringPattern("ich "), new PatternJunction("tätigkeit",
						new StringPattern("bring mich um"),
						new StringPattern("bringe mich um"),
						new StringPattern("springe von einer klippe"),
						new StringPattern("spring von einer klippe"),
						new StringPattern("vergifte mich"),
						new StringPattern("begehe toasterbad"),
						new StringPattern("nehme ein toasterbad"),
						new PatternSequence(new StringPattern("nehme ein bad mit "),
								new PatternJunction("one", new StringPattern("meinem "),new StringPattern("einem ")),
								new StringPattern("toaster")),
						new PatternSequence(new PatternJunction("schießen",
								new StringPattern("schieße "),
								new StringPattern("schieß "),
								new StringPattern("schieß' ")
						), new StringPattern("mir in den kopf"))
					),od),
				new PatternSequence(haben,new PatternJunction("lebenAbgeschlossen",
						new StringPattern("mein leben abgeschlossen"),
						new StringPattern("mit meinem leben abgeschlossen"),
						new StringPattern("abgeschlossen mit meinem leben")
					),od),
				new PatternSequence(iamgoingto,
						new OptionalPattern(new StringPattern("fucking")),
						new OptionalPattern(new StringPattern("commit ")),
						new PatternJunction("die",
								new StringPattern("die"),
								new StringPattern("kill myself"),
								new StringPattern("kms"),
								new StringPattern("suicide"),
								new StringPattern("the not living"),
								new StringPattern("not living"),
								new StringPattern("seppuku")
							),od
					),
				new PatternSequence(new StringPattern("i "),
						new OptionalPattern(new StringPattern("just ")),
						new PatternJunction("wanna",
								new StringPattern("want to "),
								new StringPattern("wanna ")
							),
						new StringPattern("die"),od
					),
				muffinSong
			);
		kmsSmokeTests();
		
		howMany = new PatternSequence(
				new StringPattern("wie oft "),
				new PatternJunction("tot",
						new PatternSequence(
								new StringPattern("habe ich mich "),
								new OptionalPattern(new StringPattern("schon ")),
								new PatternJunction("Methode",
										new StringPattern("umgebracht"),
										new StringPattern("getötet")
										)
							),
						new PatternSequence(
								new StringPattern("bin ich "),
								new OptionalPattern(new StringPattern("schon ")),
								new StringPattern("gestorben")
							)
					),
				new OptionalPattern(new StringPattern("?"))
			);
	}
	*/
	
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
