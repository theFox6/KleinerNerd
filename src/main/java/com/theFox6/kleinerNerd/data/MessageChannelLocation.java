package com.theFox6.kleinerNerd.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.theFox6.kleinerNerd.GuildNotFoundException;
import com.theFox6.kleinerNerd.TextChannelIdInGuildNotFoundException;
import com.theFox6.kleinerNerd.storage.ChannelTypeSerializer;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class MessageChannelLocation implements Comparable<MessageChannelLocation> {
	//warning: not json deserializable yet cuz creator constructor is missing
	
	//@JsonDeserialize(using = ChannelTypeDeserializer.class)
	@JsonSerialize(using = ChannelTypeSerializer.class)
	public final ChannelType type;
	@JsonProperty
	public final String channelId;
	@JsonProperty
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

	public MessageChannelLocation(MessageChannel chan) {
		this.type = chan.getType();
		this.channelId = chan.getId();
		if (chan instanceof TextChannel)
			this.guildId = ((TextChannel) chan).getGuild().getId();
		else //perhaps add support for the other guild channels too
			this.guildId = null;
		if (type == ChannelType.TEXT)
			if (guildId == null)
				throw new IllegalArgumentException("got MessageChannel of type TEXT that is not a TextChannel");
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
			if (l.channelId != null)
				return false;
		} else if (!channelId.equals(l.channelId))
			return false;
		
		if (guildId == null) {
			if (l.guildId != null)
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
		if (guildId == null) {
			return type.hashCode()^channelId.hashCode();
		} else {
			return type.hashCode()^channelId.hashCode()^guildId.hashCode();
		}
	}

    public void fetchChannel(JDA jda, Consumer<? super MessageChannel> success, Consumer<? super Throwable> fail) {
		switch (type) {
			case TEXT:
				Guild g = jda.getGuildById(guildId);
				if (g == null) {
					fail.accept(GuildNotFoundException.forGuildId(guildId));
					return;
				}
				TextChannel c = g.getTextChannelById(channelId);
				if (c == null) {
					c = jda.getTextChannelById(channelId);
					if (c != null) {
						if (c.getGuild().getId().equals(guildId)) {
							QueuedLog.info("Guild.getTextChannelById didn't find the channel");
							success.accept(c);
							return;
						} else
							QueuedLog.debug("Found a TextChannel with the right ID in the wrong guild.");
					}
					fail.accept(new TextChannelIdInGuildNotFoundException(channelId,guildId,g.getName()));
					return;
				}
				success.accept(c);
				return;
			case PRIVATE:
				PrivateChannel chan = jda.getPrivateChannelById(channelId);
				if (chan != null) {
					success.accept(chan);
					return;
				}
				jda.openPrivateChannelById(channelId).queue(success,fail);
			default:
				fail.accept(new Exception("unimplemented channel type: " + type.name()));
		}
    }

	@Override
	public int compareTo(@NotNull MessageChannelLocation o) {
		//TODO: weigh summands
		if (guildId == null || o.guildId == null)
			return type.compareTo(o.type) + channelId.compareTo(o.channelId);
		else
			return type.compareTo(o.type) + guildId.compareTo(o.guildId) + channelId.compareTo(o.channelId);
	}

	public String toString() {
		if (guildId == null) {
			return "[MessageChannelLocation for channelId " + channelId + " of type " + type.name() + "]";
		}
		return "[MessageChannelLocation for channelId " + channelId + " of type " + type.name() + " in guildId " + guildId + "]";
	}
}
