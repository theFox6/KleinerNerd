package com.theFox6.kleinerNerd.listeners;

import java.time.Instant;

import com.theFox6.kleinerNerd.KleinerNerd;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class StalkerCommandListener {
	private static final String avatarCommand = KleinerNerd.prefix + "avatar ";
	
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
		Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	if (raw.startsWith(avatarCommand)) {
    		if (msg.getMentionedUsers().isEmpty()) {
    			MessageChannel chan = event.getChannel();
	    		try {
		    		event.getJDA().retrieveUserById(
		    				raw.substring(avatarCommand.length()))
		    		.queue((u) -> {
		    			sendAvatarEmbed(msg,u);
		    		}, (e) -> {
		    			if (e.getMessage().equals("10013: Unknown User")) {
		    				QueuedLog.debug("couldn't find the user with the given id");
		    				chan.sendMessage("Discord could not find a user with that ID.").queue();
		    			} else {
			    			QueuedLog.warning("couldn't retrieve user", e);
			    			chan.sendMessage("error " + e.getMessage()).queue();
		    			}
		    		});
	    		} catch (NumberFormatException e) {
	    			QueuedLog.debug("user misformatted number: "+e.getMessage());
	    			chan.sendMessage(e.getMessage()).queue();
	    		}
    		} else {
    			msg.getMentionedUsers().forEach((u) -> sendAvatarEmbed(msg,u));
    		}
		}
	}

	private void sendAvatarEmbed(Message request,User u) {
		User author = request.getAuthor();
		EmbedBuilder avatarEmbed = new EmbedBuilder()
				.setTitle("avatar of "+u.getAsTag())
				.setTimestamp(Instant.now())
				.setImage(u.getEffectiveAvatarUrl());
		if (!request.isFromType(ChannelType.PRIVATE))
			avatarEmbed.setFooter("requested by " + author.getAsTag(),author.getEffectiveAvatarUrl());
		request.getChannel().sendMessage(avatarEmbed.build()).queue();
	}
}
