package com.theFox6.kleinerNerd.listeners;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class ConvoSnippetListener {
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
		Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	String lowerRaw = raw.toLowerCase();
    	if (lowerRaw.contains("baum") || lowerRaw.contains(" tree ")) {
    		QueuedLog.verbose("Baum");
    		msg.addReaction(Emoji.fromUnicode("U+1f333")).queue();
    	}
    	if (lowerRaw.contains("cookie") || lowerRaw.contains("keks")) {
    		QueuedLog.verbose("cookie");
    		msg.addReaction(Emoji.fromUnicode("U+1f36a")).queue();
    	} else if (lowerRaw.equals("oi")) {
    		msg.getChannel().sendMessage("Ooi").queue();
    	} else if (lowerRaw.equals("oooi")) {
    		msg.getChannel().sendMessage("Ooooi!").queue();
    	}
    }
}
