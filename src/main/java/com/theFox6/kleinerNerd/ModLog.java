package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ModLog {
	//guildid, message
	private static Map<String, Set<String>> sentMessages = new ConcurrentHashMap<>();

	//TODO: ask ownsers: send a message with buttons to the owners

	public static void sendToOwners(JDA jda, String msg) {
		ConfigFiles.getOwners().forEach((id) -> jda.retrieveUserById(id).queue(
				(user) -> user.openPrivateChannel().queue(
						(channel) -> channel.sendMessage(msg).queue((s) -> {},
								(e) -> QueuedLog.error("couldn't send message to owner", e)),
						(error) -> QueuedLog.error("couldn't open private channel to owner "+id,error)
				),
				(error) -> QueuedLog.error("couldn't retrieve owner "+id,error)
		));
	}

	private static boolean wasSentToGuild(String guildId, String msgId) {
		if (!sentMessages.containsKey(guildId))
			return false;
		return sentMessages.get(guildId).contains(msgId);
	}

	private static void addSentMessage(String guildId, String msgId) {
		if (!sentMessages.containsKey(guildId))
			sentMessages.put(guildId, ConcurrentHashMap.newKeySet());
		sentMessages.get(guildId).add(msgId);
	}

	public static void sendToGuildOnce(Guild guild, String msgId, String msg) {
		String guildId = guild.getId();
		if (wasSentToGuild(guildId, msgId))
			return;
		TextChannel chan = GuildStorage.getModLogChannel(guild);
		if (chan == null) {
			QueuedLog.debug("no log channel set for guild " + guild.getName());
			return;
		}
		chan.sendMessage(msg).queue((sm) -> addSentMessage(guildId, msgId));
	}
	
	public static void sendToGuild(JDA jda, String guildId, String msg) {
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			QueuedLog.warning("Could not find guild to log to " + guildId);
		}
		sendToGuild(guild, msg);
	}

	public static void sendToGuild(Guild guild, String msg) {
		TextChannel chan = GuildStorage.getModLogChannel(guild);
		if (chan == null) {
			QueuedLog.debug("no log channel set for guild " + guild.getName());
			return;
		}
		chan.sendMessage(msg).queue();
	}

	public static void sendToGuildOrChannel(TextChannel chan, String msg) {
		Guild guild = chan.getGuild();
		TextChannel log = GuildStorage.getModLogChannel(guild);
		if (log == null) {
			QueuedLog.debug("no log channel set for guild " + guild.getName());
			chan.sendMessage(msg).queue();
			return;
		}
		//perhaps try to send in given channel on fail
		log.sendMessage(msg).queue();
	}

	/**
	 * sets the channel to send moderator only log messages to
	 * @param guild the guild the channel is set for
	 * @param chan the new mod log channel
	 */
	public static void setModLogChannel(Guild guild, @Nullable MessageChannel chan) {
		if (chan == null)
			GuildStorage.setModLogChannel(guild, null);
		else {
			TextChannel tc = guild.getTextChannelById(chan.getId());
			if (tc == null)
				throw new IllegalArgumentException("given channel is not a text channel");
			GuildStorage.setModLogChannel(guild, tc);
		}
	}
}
