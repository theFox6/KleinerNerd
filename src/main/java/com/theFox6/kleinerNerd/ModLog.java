package com.theFox6.kleinerNerd;

import com.theFox6.kleinerNerd.storage.ConfigFiles;
import com.theFox6.kleinerNerd.storage.GuildStorage;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;

public class ModLog {
	public static void sendToOwners(JDA jda, String msg) {
		ConfigFiles.getOwners().forEach((id) -> {
			jda.retrieveUserById(id).queue(
					(user) -> user.openPrivateChannel().queue(
							(channel) -> channel.sendMessage(msg).queue((s) -> {}, (e) -> {
								QueuedLog.error("couldn't send message to owner", e);
							}),
							(error) -> QueuedLog.error("couldn't open private channel to owner "+id,error)
					),
					(error) -> QueuedLog.error("couldn't retrieve owner "+id,error)
			);
		});
	}
	
	public static void sendToGuild(JDA jda, String guildId, String msg) {
		String channelId = GuildStorage.getSettings(guildId).getModLogChannel();
		if (channelId == null) {
			QueuedLog.debug("no log channel set for guild " + guildId);
			return;
		}
		Guild guild = jda.getGuildById(guildId);
		if (guild == null) {
			QueuedLog.warning("Could not find guild to log to " + guildId);
		}
		sendToGuild(guild, channelId, msg);
	}

	public static void sendToGuild(Guild guild, String msg) {
		String channelId = GuildStorage.getSettings(guild.getId()).getModLogChannel();
		if (channelId == null) {
			QueuedLog.debug("no log channel set for guild " + guild.getName());
			return;
		}
		sendToGuild(guild,channelId,msg);
	}
	
	private static void sendToGuild(Guild guild, String channelId, String msg) {
		TextChannel chan = guild.getTextChannelById(channelId);
		if (chan == null) {
			QueuedLog.warning("could not retrieve log channel " + channelId + " in guild " + guild.getName());
			return;
		}
		chan.sendMessage(msg).queue();
	}
	
	public static void sendToGuildOrChannel(TextChannel chan, String msg) {
		Guild guild = chan.getGuild();
		String logChannelId = GuildStorage.getSettings(guild.getId()).getModLogChannel();
		if (logChannelId == null) {
			QueuedLog.info("no log channel set for guild " + guild.getName());
			chan.sendMessage(msg).queue();
			return;
		}
		TextChannel log = guild.getTextChannelById(logChannelId);
		if (log == null) {
			QueuedLog.warning("could not retrieve log channel " + logChannelId + " in guild " + guild.getName());
			chan.sendMessage(msg).queue();
			return;
		}
		//perhaps try to send in given channel on fail
		log.sendMessage(msg).queue();
	}

	public static void setModLogChannel(Guild guild, TextChannel chan) {
		String gid = guild.getId();
		if (!gid.equals(chan.getGuild().getId())) {
			throw new IllegalArgumentException("given channel is not within given guild");
		}
		GuildStorage.getSettings(gid).setModLogChannel(chan.getId());
	}
}
