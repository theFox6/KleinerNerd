package com.theFox6.kleinerNerd.data;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

public class MessageLocation extends MessageChannelLocation {

	public final String messageId;

	public MessageLocation(Message msg) {
		super(msg.getChannelType(),msg.getChannel().getId(),msg.isFromGuild() ? msg.getGuild().getId() : null);
		messageId = msg.getId();
	}

	public MessageLocation(GenericMessageEvent event) {
		super(event.getChannelType(), event.getChannel().getId(), event.isFromGuild() ? event.getGuild().getId() : null);
		messageId = event.getMessageId();
	}

	public boolean equals(Object o) {
		if (o == this)
			return true;
		
		if (!(o instanceof MessageLocation))
			return false;
		MessageLocation l = (MessageLocation) o;
		
		if (!super.equals(l))
			return false;
		
		if (!messageId.equals(l.messageId))
			return false;
		
		return true;
	}

	public int hashCode() {
		return super.hashCode()^messageId.hashCode();
	}
}
