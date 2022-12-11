package com.theFox6.kleinerNerd.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.theFox6.kleinerNerd.GuildNotFoundException;
import com.theFox6.kleinerNerd.TextChannelIdInGuildNotFoundException;
import com.theFox6.kleinerNerd.storage.ChannelTypeSerializer;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
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
		//TODO: support for other channel types
		if (type == ChannelType.TEXT) {
			this.channelId = channelId;
			this.guildId = guildId;
		} else if (type == ChannelType.PRIVATE) {
			if (guildId != null)
				throw new IllegalArgumentException("Didn't expect two arguments for a private channel.");
			this.channelId = channelId;
			this.guildId = null;
		} else {
			throw new IllegalArgumentException("Unsupported channel type: "+ channelType.name());
		}
	}

	public MessageChannelLocation(MessageChannelUnion chan) {
		this.type = chan.getType();
		this.channelId = chan.getId();
		if (chan.getType().isGuild())
			this.guildId = chan.asGuildMessageChannel().getGuild().getId();
		else
			this.guildId = null;
		//is this extra check still needed?
		if (type == ChannelType.TEXT)
			if (guildId == null)
				throw new IllegalArgumentException("got MessageChannel of type TEXT without guild id");
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
			//noinspection RedundantIfStatement
			if (l.guildId != null)
				return false;
		} else //noinspection RedundantIfStatement
			if (!guildId.equals(l.guildId))
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
		//TODO: use type.isGuild() etc. to support more channels
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

	/**
	 * Comparison method for ordering and mapping.
	 * Since discord snowflakes have 64 bits {@code (x.compareTo(y)==0)} does not imply that {@code (x.equals(y))}.
	 * @param o the other MessageChannelLocation to compare this one to
	 * @return a value that mostly (but not necessarily) differs if two MessageChannelLocations are not equal
	 */
	@Override
	public int compareTo(@NotNull MessageChannelLocation o) {
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
