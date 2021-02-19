package com.theFox6.kleinerNerd.listeners;

import javax.annotation.Nonnull;

import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ExceptionEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.StatusChangeEvent;
import net.dv8tion.jda.api.events.channel.priv.PrivateChannelCreateEvent;
import net.dv8tion.jda.api.events.channel.priv.PrivateChannelDeleteEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.update.GuildUpdateOwnerEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageUpdateEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

public class LoggingListener extends ListenerAdapter implements EventListener {
	@Override
	@SubscribeEvent
	public void onReady(ReadyEvent event) {
		QueuedLog.action("bot ready");
	}
	
	@Override
	@SubscribeEvent
	public void onResume(ResumedEvent event) {
		QueuedLog.debug("Bot resumed");
	}
	
	@Override
	@SubscribeEvent
    public void onReconnect(ReconnectedEvent event) {
		QueuedLog.action("Bot reconnected");
	}
	
	@Override
	@SubscribeEvent
    public void onDisconnect(DisconnectEvent event) {
		QueuedLog.debug("Bot disconnected");
	}
	
	@Override
	@SubscribeEvent
    public void onShutdown(ShutdownEvent event) {
		QueuedLog.action("Bot shutdown");
	}
	
	@Override
	@SubscribeEvent
    public void onStatusChange(StatusChangeEvent event) {
		QueuedLog.verbose(event.toString());
    }
    
	@Override
	@SubscribeEvent
    public void onException(ExceptionEvent event) {
    	QueuedLog.warning("exception occured", event.getCause());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	QueuedLog.verbose("message from guild " + msg.getGuild().getName() + " in channel " + msg.getChannel().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
    	Message msg = event.getMessage();
    	QueuedLog.verbose("message edited in guild " + msg.getGuild().getName() + " in channel " + msg.getChannel().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
		QueuedLog.verbose("message deleted in channel " + event.getChannel().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	User author = msg.getAuthor();
    	if (event.getJDA().getSelfUser().equals(author))
    		return;
    	QueuedLog.info("private message in conversation with " + msg.getChannel().getName() + "\n"
    			+ "written by " + author.getName() + "\n"
				+ "message contents " + msg.getContentRaw());
    }
    
	@Override
	@SubscribeEvent
    public void onPrivateMessageUpdate(@Nonnull PrivateMessageUpdateEvent event) {
    	Message msg = event.getMessage();
    	User author = msg.getAuthor();
    	if (event.getJDA().getSelfUser().equals(author))
    		return;
    	QueuedLog.info("private message edited in convo with " + msg.getChannel().getName() + "\n"
    			+ "new message contents " + msg.getContentRaw());
    }
    
	@Override
	@SubscribeEvent
    public void onPrivateMessageDelete(@Nonnull PrivateMessageDeleteEvent event) {
		QueuedLog.info("private message deleted from " + event.getChannel().getName());
    	//later also print message ID
    }
    
	@Override
	@SubscribeEvent
    public void onPrivateChannelCreate(@Nonnull PrivateChannelCreateEvent event) {
		QueuedLog.verbose("private channel created: " + event.getChannel().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onPrivateChannelDelete(@Nonnull PrivateChannelDeleteEvent event) {
		QueuedLog.info("private channel deleted: " + event.getChannel().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
		QueuedLog.action("joined guild " + event.getGuild().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildLeave(@Nonnull GuildLeaveEvent event) {
		QueuedLog.action("left guild " + event.getGuild().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildBan(@Nonnull GuildBanEvent event) {
		QueuedLog.info(event.getUser().getName() + " was banned from " + event.getGuild().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildUnban(@Nonnull GuildUnbanEvent event) {
		QueuedLog.info(event.getUser().getName() + " was unbanned from " + event.getGuild().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
		QueuedLog.info(event.getUser().getName() + " left " + event.getGuild().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildUpdateOwner(@Nonnull GuildUpdateOwnerEvent event) {
		QueuedLog.info("ownership of guild " + event.getGuild().getName() + " was transferred to " + event.getNewOwner().getUser().getName());
    }
    
	@Override
	@SubscribeEvent
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
		QueuedLog.info("member " + event.getUser().getName() + "joined" + event.getGuild().getName());
    }
}
