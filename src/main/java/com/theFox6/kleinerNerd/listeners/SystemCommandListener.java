package com.theFox6.kleinerNerd.listeners;

import com.theFox6.kleinerNerd.KleinerNerd;
import com.theFox6.kleinerNerd.MultiActionHandler;
import com.theFox6.kleinerNerd.storage.ConfigFiles;

import com.theFox6.kleinerNerd.system.InstanceManager;
import com.theFox6.kleinerNerd.system.InstanceState;
import foxLog.deamon.ConditionNotifier.NotNotifiableException;
import foxLog.queued.QueuedLog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.SubscribeEvent;

import java.io.IOException;
import java.util.List;

public class SystemCommandListener {
	@SubscribeEvent
	public void onMessageReceived(MessageReceivedEvent event) {
    	Message msg = event.getMessage();
    	String raw = msg.getContentRaw();
    	MessageChannel chan = event.getChannel();
    	String ownId = event.getJDA().getSelfUser().getId();
    	if (!raw.startsWith(KleinerNerd.prefix))
    		return;
    	if (raw.equals(KleinerNerd.prefix + "ping")) {
            long time = System.currentTimeMillis();
            chan.sendMessage("Pong!\n(waiting for an answer from Discord)")
            .queue(response -> {
            	JDA jda = response.getJDA();
            	response.editMessageFormat("Pong!\n"
            			+ "sending time: %d ms\n"
            			+ "last heartbeat: %d ms",
						System.currentTimeMillis() - time,
						jda.getGatewayPing()
				).queue();
            });
    	} else if (raw.equals(KleinerNerd.prefix + "pong")) {
    		chan.sendMessage("PENG!").queue();
    	} else if (raw.equals(KleinerNerd.prefix + "shutdown") ||
				(raw.startsWith(KleinerNerd.prefix + "shutdown") && msg.getMentionedUsers().stream().map(User::getId).anyMatch(el -> el.equals(ownId)))) {
    		if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
    			//perhaps inform the user
    			return;
    		}
    		chan.sendMessage("Shutting down").queue();
    		QueuedLog.action("Shutdown requested by " + msg.getAuthor().getName());
    		InstanceManager.setState(InstanceState.SHUTTING_DOWN);
    		event.getJDA().shutdown();
    	} else if (raw.equals(KleinerNerd.prefix + "logfile")) {
    		if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
    			return;
    		}
    		QueuedLog.action("Logfile requested by " + msg.getAuthor().getName());
    		try {
				QueuedLog.flushInterruptibly();
			} catch (InterruptedException e) {
				QueuedLog.warning("logfile request was interrupted", e);
			} catch (NotNotifiableException e) {
				//this exception doesn't even exist in the latest version...
				QueuedLog.error("Yo Fox, your log implementation is faulty.", e);
			}
    		chan.sendFile(KleinerNerd.logFile).queue();
    	} else if (raw.equals(KleinerNerd.prefix + "update") ||
				(raw.startsWith(KleinerNerd.prefix + "update") && msg.getMentionedUsers().stream().map(User::getId).anyMatch(el -> el.equals(ownId)))) {
			if (!ConfigFiles.getOwners().contains(msg.getAuthor().getId())) {
				//perhaps inform the user
				return;
			}
			chan.sendMessage("Starting update").queue();
			QueuedLog.action("Update requested by " + msg.getAuthor().getName());
			InstanceManager.setState(InstanceState.UPDATING);
			try {
				new ProcessBuilder("bash","update.sh").start();
			} catch (IOException e) {
				QueuedLog.error("Could not start update script",e);
			}
			event.getJDA().shutdown();
		}
    }
}
