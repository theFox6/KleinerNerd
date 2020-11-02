package com.theFox6.kleinerNerd.echo;

import java.util.function.Consumer;

import com.theFox6.kleinerNerd.echo.faults.*;
import com.theFox6.kleinerNerd.matchers.ChannelLocationMatch;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class PreparedEcho {
	public static enum EchoState {
		INVALID,
		STORED,
		READY,
		SENT;
	}

	/*
	public static enum ConfirmState {
		NEED,
		GIVEN,
		AUTO;
	}
	*/
	
	//TODO: use MessageChannelLocation

	private PreparedEcho.EchoState stage;
	//private ConfirmState confirm;
	private JDA jda;
	private String author;
	private ChannelType targetPlace;
	private final String guildId;
	private final String channelId;
	private final String message;
	private User user;
	private TextChannel target = null;
	
	public PreparedEcho(JDA jda, ChannelLocationMatch loc, User author) {
		this.jda = jda;
		this.author = author.getId();
		targetPlace = loc.getExpectedPlace();
		if (targetPlace == ChannelType.TEXT) {
			guildId = loc.getGuildId();
			channelId = loc.getChannelId();
			message = loc.getArgs();
			stage = EchoState.STORED;
			/*
			if (guildId == null) {
				confirm = ConfirmState.NEED;
			} else {
				confirm = ConfirmState.AUTO;
			}
			*/
		} else if (targetPlace == ChannelType.PRIVATE) {
			guildId = null;
			channelId = loc.getChannelId();
			message = loc.getArgs();
			stage = EchoState.STORED;
		} else if (targetPlace == null) {
			guildId = loc.getGuildId();
			channelId = loc.getChannelId();
			message = loc.getArgs();
			stage = EchoState.STORED;
			QueuedLog.debug("PreparedEcho got no target place");
			//throw new NullPointerException("ChannelLocationMatch provided no targetPlace");
		} else {
			guildId = loc.getGuildId();
			channelId = loc.getChannelId();
			message = loc.getArgs();
			stage = EchoState.INVALID;
			QueuedLog.warning("PreparedEcho didn't expect target place " + targetPlace.name());
		}
	}

	public void retrieveChannel() throws ChannelRetrievalException, InvalidTargetPlaceException {
		if (stage != EchoState.STORED) {
			QueuedLog.warning("called retrieveChannel() on " + stage.name() + " PreparedEcho");
		}
		if (targetPlace == ChannelType.TEXT) {
			TextChannel channel;
			if (guildId == null) {
				channel = jda.getTextChannelById(channelId);
				if (channel == null) {
					stage = EchoState.INVALID;
					throw new TextChannelIdNotFoundException(channelId);
				}
				target = channel;
				stage = EchoState.READY;
			} else {
				Guild guild = jda.getGuildById(guildId);
				if (guild == null) {
					stage = EchoState.INVALID;
					throw new GuildIdNotFoundException(guildId);
				}
				channel = guild.getTextChannelById(channelId);
				if (target == null) {
					stage = EchoState.INVALID;
					throw new TextChannelIdInGuildNotFoundException(channelId, guildId, guild.getName());
				}
				target = channel;
				stage = EchoState.READY;
			}
			QueuedLog.action("Prepared echo message to #" + channel.getName() + "@" + channel.getGuild().getName());
		} else if (targetPlace == ChannelType.PRIVATE) {
			user = jda.getUserById(channelId);
			if (user == null) {
				stage = EchoState.INVALID;
				throw new UserIdNotFoundException(channelId);
			}
			QueuedLog.action("Prepared echo message to " + user.getAsTag());
			stage = EchoState.READY;
		} else if (targetPlace == null) {
			if (guildId == null) {
				if (channelId == null) {
					throw new NoTargetPlaceException("didn't find a target place in the parsed command");
				} else {
					TextChannel channel = jda.getTextChannelById(channelId);
					if (channel == null) {
						user = jda.getUserById(channelId);
						if (user == null) {
							stage = EchoState.INVALID;
							throw new NoTargetPlaceException("couldn't find a place with the given ID");
						}
						QueuedLog.action("Prepared echo message to " + user.getAsTag());
						stage = EchoState.READY;
					} else {
						target = channel;
						stage = EchoState.READY;
					}
				}
			} else {
				Guild guild = jda.getGuildById(guildId);
				if (guild == null) {
					stage = EchoState.INVALID;
					throw new GuildIdNotFoundException(guildId);
				}
				TextChannel channel = guild.getTextChannelById(channelId);
				if (target == null) {
					stage = EchoState.INVALID;
					throw new TextChannelIdInGuildNotFoundException(channelId, guildId, guild.getName());
				}
				target = channel;
				stage = EchoState.READY;
				QueuedLog.action("Prepared echo message to #" + channel.getName() + "@" + channel.getGuild().getName());
			}
		} else {
			stage = EchoState.INVALID;
			throw new InvalidTargetPlaceException("retrieveChannel didn't expect target place " + targetPlace.name());
		}
	}

	public MessageEmbed createConfirmEmbed() {
		EmbedBuilder embed = new EmbedBuilder()
				.setTitle("send echo message");
		if (target != null) {
			embed.addField("target guild text channel",
					"#" + target.getName() + " in " + target.getGuild().getName(),true);
		}
		if (user != null) {
			embed.addField("target DM",user.getAsTag(),true);
		}
		embed.addField("message contents",message,false);
		embed.setFooter("Please confirm or reject.");
		return embed.build();
	}

	public void sendConfirmEmbed(MessageChannel chan, Consumer<Message> success) {
		chan.sendMessage(createConfirmEmbed()).queue((msg) -> {
			msg.addReaction("U+2611").queue();
			msg.addReaction("U+274c").queue();
			success.accept(msg);
		});
	}

	public void send(Consumer<Message> success) {
		if (stage != EchoState.READY) {
			QueuedLog.warning("called send() on " + stage.name() + " PreparedEcho");
		}
		QueuedLog.info("sending echo message:\n" + message);
		if (user != null) {
			user.openPrivateChannel().queue((channel) -> channel.sendMessage(message).queue(success));
		}
		if (target != null) {
			target.sendMessage(message).queue(success);
		}
		stage = EchoState.SENT;
	}

	public boolean isInvalid() {
		return stage == EchoState.INVALID;
	}

	public String getAuthorId() {
		return author;
	}
}