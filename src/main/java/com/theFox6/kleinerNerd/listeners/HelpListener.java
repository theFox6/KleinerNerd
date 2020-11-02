package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class HelpListener {

	public static final String helpCommand = KleinerNerd.prefix + "help ";
	
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
		Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	if (raw.equals(KleinerNerd.prefix + "help")) {
    		chan.sendMessage("Help!").queue();
    		//maybe there will be help later
    	} else if (raw.startsWith(helpCommand)) {
    		String topic = raw.substring(helpCommand.length());
    		if (topic.equals("location format")) {
    			chan.sendMessage("Locations should be provided in the format:\n" +
    					"`PRIVATE userID` or `TEXT guildID channelID`").queue();
    		} else {
    			chan.sendMessage("Got no help for: " + topic).queue(
    					(m) -> chan.sendMessage("Sorry.").queue());
    		}
    	}
	}

}
