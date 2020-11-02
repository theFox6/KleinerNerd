package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class SimpleCommandListener {
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	if (!raw.startsWith(KleinerNerd.prefix))
    		return;
    	if (raw.equals(KleinerNerd.prefix + "ping")) {
            long time = System.currentTimeMillis();
            chan.sendMessage("Pong!")
            .queue(response -> {
            	response.editMessageFormat("Pong: %d ms", System.currentTimeMillis() - time).queue();
            });
    	} else if (raw.equals(KleinerNerd.prefix + "shutdown")) {
    		chan.sendMessage("Shutting down").queue();
    		QueuedLog.action("Shutdown requested by " + msg.getAuthor().getName());
    		event.getJDA().shutdown();
    	}
    }
}
