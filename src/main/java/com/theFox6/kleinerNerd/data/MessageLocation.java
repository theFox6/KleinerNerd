package com.theFox6.kleinerNerd.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.guild.GenericGuildMessageEvent;

public class MessageLocation extends MessageChannelLocation {
	@JsonProperty
	public final String messageId;
	
	@JsonCreator
	private MessageLocation(@JsonProperty("type") String chType,
			@JsonProperty("channelId") String chanId,
			@JsonProperty("guildId") String gId,
			@JsonProperty("messageId") String msgId) {
		super(ChannelType.valueOf(chType),chanId, gId);
		messageId = msgId;
	}

	public MessageLocation(Message msg) {
		super(msg.getChannelType(),msg.getChannel().getId(),msg.isFromGuild() ? msg.getGuild().getId() : null);
		messageId = msg.getId();
	}

	public MessageLocation(GenericMessageEvent event) {
		super(event.getChannelType(), event.getChannel().getId(), event.isFromGuild() ? event.getGuild().getId() : null);
		messageId = event.getMessageId();
	}
	
	public MessageLocation(GenericGuildMessageEvent event) {
		super(ChannelType.TEXT, event.getChannel().getId(), event.getGuild().getId());
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
