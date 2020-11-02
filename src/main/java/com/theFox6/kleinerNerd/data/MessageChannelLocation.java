package com.theFox6.kleinerNerd.data;

import net.dv8tion.jda.api.entities.ChannelType;

public class MessageChannelLocation {
	public final ChannelType type;
	public final String channelId;
	public final String guildId;
	
	public MessageChannelLocation(ChannelType channelType, String userId) {
		type = channelType;
		if (type == ChannelType.PRIVATE) {
			channelId = userId;
			guildId = null;
		} else if (type == ChannelType.TEXT) {
			channelId = userId;
			throw new IllegalArgumentException("No guildId provided.");
		} else {
			throw new IllegalArgumentException("Unsupported channel type: "+ channelType.name());
		}
	}
	
	public MessageChannelLocation(ChannelType channelType, String channelId, String guildId) {
		type = channelType;
		if (type == ChannelType.TEXT) {
			this.channelId = channelId;
			this.guildId = guildId;
		} else if (type == ChannelType.PRIVATE) {
			if (guildId != null)
				throw new IllegalArgumentException("Didn't expect two arguments for a private channel.");
			this.channelId = channelId;
			this.guildId = guildId;
		} else {
			throw new IllegalArgumentException("Unsupported channel type: "+ channelType.name());
		}
	}
	
	public boolean equals(Object o) {
		if (o == this)
			return true;
		
		if (!(o instanceof MessageChannelLocation))
			return false;
		MessageChannelLocation l = (MessageChannelLocation) o;
		
		if (type != l.type)
			return false;
		
		if (channelId == null) {
			if (channelId != l.channelId)
				return false;
		} else if (!channelId.equals(l.channelId))
			return false;
		
		if (guildId == null) {
			if (guildId != l.guildId)
				return false;
		} else if (!guildId.equals(l.guildId))
			return false;
		
		return true;
	}

	/**
	 * Calculate a hash code for the MessageChannelLocation.
	 * It is made by applying an exclusive-or on the hash codes of all fields.
	 */
	public int hashCode() {
		return type.hashCode()^channelId.hashCode()^guildId.hashCode();
	}
}
