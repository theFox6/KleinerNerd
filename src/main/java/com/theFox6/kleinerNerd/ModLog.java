package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.storage.GuildStorage;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;

import javax.annotation.Nullable;

public class ModLog {
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
