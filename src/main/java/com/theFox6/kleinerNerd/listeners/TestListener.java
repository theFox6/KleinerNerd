package com.theFox6.kleinerNerd.listeners;

import java.util.List;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.matchers.ChannelStartMatch;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class TestListener extends ListenerAdapter implements EventListener {
	public static final String chanstartCommand = KleinerNerd.prefix + "chanstart ";
	
	@Override
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	if (raw.startsWith(KleinerNerd.prefix + "chanlist")) {
    		List<TextChannel> chans = msg.getMentionedChannels();
    		String list = "";
    		for (TextChannel target : chans) {
    			list += target.getName() + ",";
    		}
    		chan.sendMessage("Channel names: " + list).queue();
    	} else if (raw.startsWith(chanstartCommand)) {
    		ChannelStartMatch chanMatch = ChannelStartMatch.matchCommand(raw,chanstartCommand);
    		
    		if (chanMatch.isFullMatch()) {
	    		chan.sendMessage("channel ID mentioned:\n"+chanMatch.getChannelID()).queue();
	    		chan.sendMessage("message after channel mention:\n" + chanMatch.getArgs()).queue();
    		} else {
    			chan.sendMessage("no channel at start, got args:\n" + chanMatch.getArgs()).queue();
    		}
    	}
    }
}
